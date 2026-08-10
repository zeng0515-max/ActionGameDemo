package com.actiongame.server.benchmark;

import com.actiongame.server.audit.AuditLoggerImpl;
import com.actiongame.server.net.WebSocketServer;
import com.actiongame.server.net.handler.HandlerRegistry;
import com.actiongame.server.net.handler.HeartbeatHandler;
import com.actiongame.server.net.handler.JoinRoomHandler;
import com.actiongame.server.net.handler.LoginHandler;
import com.actiongame.server.net.handler.PlayerActionHandler;
import com.actiongame.server.net.session.ConnectionManager;
import io.netty.channel.nio.NioEventLoopGroup;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class BenchmarkRunner {
    private BenchmarkRunner() {}

    public static void main(String[] args) throws Exception {
        int clients = args.length > 0 ? Integer.parseInt(args[0]) : 16;
        long durationSec = args.length > 1 ? Long.parseLong(args[1]) : 10;
        int actionIntervalMs = args.length > 2 ? Integer.parseInt(args[2]) : 50;
        int port = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        String outputPath = args.length > 4 ? args[4]
            : Path.of("docs", "benchmark", "BENCHMARK_" + LocalDate.now() + ".md").toString();

        BenchmarkReport report = run(clients, durationSec, actionIntervalMs, port);
        report.writeTo(Path.of(outputPath));
        System.out.println(report.toMarkdown());
        System.out.println("Report written to " + outputPath);
    }

    public static BenchmarkReport run(int clients, long durationSec, int actionIntervalMs, int port)
            throws Exception {
        ConnectionManager connectionManager = new ConnectionManager();
        HandlerRegistry registry = new HandlerRegistry(connectionManager);
        AuditLoggerImpl auditLogger = new AuditLoggerImpl(
            Path.of(System.getProperty("java.io.tmpdir"), "actiongame-benchmark-logs"));
        registry.register(new LoginHandler(connectionManager, auditLogger));
        registry.register(new HeartbeatHandler());
        registry.register(new JoinRoomHandler(connectionManager, auditLogger));
        registry.register(new PlayerActionHandler(connectionManager, auditLogger));

        WebSocketServer server = new WebSocketServer(port, connectionManager, registry);
        server.start();
        int actualPort = server.getPort();

        LoadTestConfig config = new LoadTestConfig(
            clients, durationSec * 1000L, actionIntervalMs, 4, "127.0.0.1", actualPort);
        NioEventLoopGroup group = new NioEventLoopGroup();
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(2, clients));

        var cpuSum = new AtomicLong();
        var cpuSamples = new AtomicLong();
        var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Thread sampler = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                double load = os.getProcessCpuLoad();
                if (load >= 0) {
                    cpuSum.addAndGet((long) (load * 1000));
                    cpuSamples.incrementAndGet();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        sampler.setDaemon(true);
        sampler.start();

        long start = System.currentTimeMillis();
        List<Future<LoadTestClientStats>> futures = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            LoadTestClient client = new LoadTestClient(config, i);
            futures.add(executor.submit(() -> client.run(group)));
        }

        List<LoadTestClientStats> results = new ArrayList<>();
        for (Future<LoadTestClientStats> future : futures) {
            results.add(future.get(60, TimeUnit.SECONDS));
        }
        long elapsed = System.currentTimeMillis() - start;

        sampler.interrupt();
        executor.shutdownNow();
        group.shutdownGracefully().sync();
        server.shutdown();

        double avgCpu = cpuSamples.get() == 0 ? 0 : cpuSum.get() / (1000.0 * cpuSamples.get());
        return new BenchmarkReport(config, results, elapsed, avgCpu);
    }
}
