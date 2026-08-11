package com.actiongame.server;

import com.actiongame.server.net.WebSocketServer;
import com.actiongame.server.net.handler.HandlerRegistry;
import com.actiongame.server.net.handler.HeartbeatHandler;
import com.actiongame.server.net.handler.JoinRoomHandler;
import com.actiongame.server.net.handler.LoginHandler;
import com.actiongame.server.net.handler.PlayerActionHandler;
import com.actiongame.server.net.session.ConnectionManager;
import com.actiongame.server.room.RoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {

    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
    private static final int DEFAULT_PORT = 9090;
    private static final int DEFAULT_METRICS_PORT = 9091;

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = parsePort(args[0], DEFAULT_PORT, "port");
        } else {
            port = parsePort(System.getenv("SERVER_PORT"), DEFAULT_PORT, "SERVER_PORT");
        }

        int metricsPort = parsePort(System.getenv("METRICS_PORT"), DEFAULT_METRICS_PORT, "METRICS_PORT");
        String nodeId = System.getenv("NODE_ID");
        if (nodeId == null || nodeId.isBlank()) {
            nodeId = "local";
        }

        log.info("=== ActionGameDemo Server ===");
        log.info("Node id: {}", nodeId);
        log.info("Starting server on port {}, metrics on {}...", port, metricsPort);

        ConnectionManager connectionManager = new ConnectionManager();

        // 审计日志
        com.actiongame.server.audit.AuditLoggerImpl auditLogger = new com.actiongame.server.audit.AuditLoggerImpl();
        auditLogger.logSystem("Server booting on port " + port);
        log.info("Audit logger initialized");

        HandlerRegistry handlerRegistry = new HandlerRegistry(connectionManager);

        RoomManager roomManager = RoomManager.getInstance();
        log.info("Room router ready on node {}", roomManager.getNodeId());
        roomManager.start();

        // 注册消息处理器 (注入 auditLogger)
        handlerRegistry.register(new LoginHandler(connectionManager, auditLogger));
        handlerRegistry.register(new HeartbeatHandler());
        handlerRegistry.register(new JoinRoomHandler(connectionManager, auditLogger));
        handlerRegistry.register(new PlayerActionHandler(connectionManager, auditLogger));

        var metrics = auditLogger.getMetrics();
        WebSocketServer server = new WebSocketServer(port, connectionManager, handlerRegistry, metrics);
        com.actiongame.server.observability.MetricsHttpServer metricsServer =
            new com.actiongame.server.observability.MetricsHttpServer(
                metricsPort, metrics, new com.actiongame.server.observability.AlertEvaluator(), roomManager);
        metricsServer.start();
        log.info("Metrics server listening on http://0.0.0.0:{}/metrics", metricsServer.getPort());
        server.start();

        log.info("Server started on port {}", port);
        log.info("Waiting for client connections...");

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            roomManager.stop();
            metricsServer.stop();
            server.shutdown();
            log.info("Server stopped.");
        }));

        // 阻塞主线程
        Thread.currentThread().join();
    }

    private static int parsePort(String rawValue, int defaultValue, String source) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            log.warn("Invalid {} '{}', using default {}", source, rawValue, defaultValue);
            return defaultValue;
        }
    }
}
