package com.actiongame.server;

import com.actiongame.server.net.WebSocketServer;
import com.actiongame.server.net.handler.HandlerRegistry;
import com.actiongame.server.net.handler.HeartbeatHandler;
import com.actiongame.server.net.handler.JoinRoomHandler;
import com.actiongame.server.net.handler.LoginHandler;
import com.actiongame.server.net.handler.PlayerActionHandler;
import com.actiongame.server.net.session.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {

    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
    private static final int DEFAULT_PORT = 9090;
    private static final int DEFAULT_METRICS_PORT = 9091;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                log.warn("Invalid port '{}', using default {}", args[0], DEFAULT_PORT);
            }
        }

        int metricsPort = DEFAULT_METRICS_PORT;
        String metricsPortEnv = System.getenv("METRICS_PORT");
        if (metricsPortEnv != null && !metricsPortEnv.isBlank()) {
            try {
                metricsPort = Integer.parseInt(metricsPortEnv);
            } catch (NumberFormatException e) {
                log.warn("Invalid METRICS_PORT '{}', using default {}", metricsPortEnv, DEFAULT_METRICS_PORT);
            }
        }

        log.info("=== ActionGameDemo Server ===");
        log.info("Starting server on port {}...", port);

        ConnectionManager connectionManager = new ConnectionManager();

        // 审计日志
        com.actiongame.server.audit.AuditLoggerImpl auditLogger = new com.actiongame.server.audit.AuditLoggerImpl();
        auditLogger.logSystem("Server booting on port " + port);
        log.info("Audit logger initialized");

        HandlerRegistry handlerRegistry = new HandlerRegistry(connectionManager);

        // 注册消息处理器 (注入 auditLogger)
        handlerRegistry.register(new LoginHandler(connectionManager, auditLogger));
        handlerRegistry.register(new HeartbeatHandler());
        handlerRegistry.register(new JoinRoomHandler(connectionManager, auditLogger));
        handlerRegistry.register(new PlayerActionHandler(connectionManager, auditLogger));

        var metrics = auditLogger.getMetrics();
        WebSocketServer server = new WebSocketServer(port, connectionManager, handlerRegistry, metrics);
        com.actiongame.server.observability.MetricsHttpServer metricsServer =
            new com.actiongame.server.observability.MetricsHttpServer(
                metricsPort, metrics, new com.actiongame.server.observability.AlertEvaluator());
        metricsServer.start();
        log.info("Metrics server listening on http://0.0.0.0:{}/metrics", metricsServer.getPort());
        server.start();

        log.info("Server started on port {}", port);
        log.info("Waiting for client connections...");

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            metricsServer.stop();
            server.shutdown();
            log.info("Server stopped.");
        }));

        // 阻塞主线程
        Thread.currentThread().join();
    }
}
