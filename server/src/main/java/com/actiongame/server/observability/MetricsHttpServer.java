package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import com.actiongame.server.room.RoomManager;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MetricsHttpServer {
    private static final Gson GSON = new Gson();

    private final HttpServer server;
    private final ServerMetrics metrics;
    private final AlertEvaluator alertEvaluator;
    private final RoomManager roomManager;

    public MetricsHttpServer(int port, ServerMetrics metrics, AlertEvaluator alertEvaluator) throws IOException {
        this(port, metrics, alertEvaluator, null);
    }

    public MetricsHttpServer(int port, ServerMetrics metrics, AlertEvaluator alertEvaluator,
                             RoomManager roomManager) throws IOException {
        this.metrics = metrics;
        this.alertEvaluator = alertEvaluator;
        this.roomManager = roomManager;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext("/ready", this::handleReady);
        server.createContext("/metrics", this::handleMetrics);
        server.createContext("/alerts", this::handleAlerts);
        server.createContext("/routing/room/", this::handleRoomRouting);
        server.createContext("/admin/drain", this::handleDrain);
        server.createContext("/admin/undrain", this::handleUndrain);
        server.createContext("/admin/status", this::handleDrainStatus);
        server.createContext("/admin/migrate/", this::handleMigrateRoom);
        server.createContext("/admin/migrate-all", this::handleMigrateAll);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        String body = "{\"status\":\"ok\",\"activeConnections\":" + metrics.getCurrentConnections() + "}";
        writeJson(exchange, body);
    }

    private void handleReady(HttpExchange exchange) throws IOException {
        if (roomManager != null && roomManager.isDraining()) {
            writeJson(exchange, "{\"status\":\"draining\"}", 503);
            return;
        }
        writeJson(exchange, "{\"status\":\"ready\"}");
    }

    private void handleDrain(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        if (roomManager == null) {
            writeJson(exchange, "{\"error\":\"room manager unavailable\"}", 503);
            return;
        }
        roomManager.setDraining(true);
        writeJson(exchange, GSON.toJson(Map.of(
            "status", "draining",
            "activeRooms", roomManager.getRoomCount()
        )));
    }

    private void handleUndrain(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        if (roomManager == null) {
            writeJson(exchange, "{\"error\":\"room manager unavailable\"}", 503);
            return;
        }
        roomManager.setDraining(false);
        writeJson(exchange, "{\"status\":\"ready\"}");
    }

    private void handleDrainStatus(HttpExchange exchange) throws IOException {
        if (roomManager == null) {
            writeJson(exchange, "{\"error\":\"room manager unavailable\"}", 503);
            return;
        }
        writeJson(exchange, GSON.toJson(Map.of(
            "draining", roomManager.isDraining(),
            "activeRooms", roomManager.getRoomCount()
        )));
    }

    private void handleMigrateRoom(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        if (roomManager == null) {
            writeJson(exchange, "{\"error\":\"room manager unavailable\"}", 503);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String prefix = "/admin/migrate/";
        String roomId = URLDecoder.decode(path.substring(prefix.length()), StandardCharsets.UTF_8);
        roomManager.migrateRoom(roomId);
        writeJson(exchange, GSON.toJson(Map.of("status", "migrated", "roomId", roomId)));
    }

    private void handleMigrateAll(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        if (roomManager == null) {
            writeJson(exchange, "{\"error\":\"room manager unavailable\"}", 503);
            return;
        }
        int migrated = roomManager.migrateAllRooms();
        writeJson(exchange, GSON.toJson(Map.of("status", "migrated", "count", migrated)));
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        writeText(exchange, prometheusText());
    }

    private void handleAlerts(HttpExchange exchange) throws IOException {
        writeJson(exchange, GSON.toJson(Map.of("alerts", alertEvaluator.evaluate(metrics))));
    }

    private void handleRoomRouting(HttpExchange exchange) throws IOException {
        if (roomManager == null) {
            writeJson(exchange, "{\"error\":\"room routing is not enabled\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String prefix = "/routing/room/";
        String encodedRoomId = path.substring(prefix.length());
        String roomId = URLDecoder.decode(encodedRoomId, StandardCharsets.UTF_8);
        String owner = roomManager.getRoomOwner(roomId);
        String address = owner == null || owner.isBlank() ? "" : roomManager.resolveNodeAddress(owner);

        writeJson(exchange, GSON.toJson(Map.of(
            "roomId", roomId,
            "nodeId", owner == null ? "" : owner,
            "address", address,
            "status", owner == null ? "unassigned" : "assigned"
        )));
    }

    private String prometheusText() {
        return "# HELP actiongame_connections_active Active WebSocket connections\n"
            + "# TYPE actiongame_connections_active gauge\n"
            + "actiongame_connections_active " + metrics.getCurrentConnections() + "\n"
            + "# HELP actiongame_rooms_active Active battle rooms\n"
            + "# TYPE actiongame_rooms_active gauge\n"
            + "actiongame_rooms_active " + metrics.getActiveRooms() + "\n"
            + "# HELP actiongame_messages_received_total Received messages\n"
            + "# TYPE actiongame_messages_received_total counter\n"
            + "actiongame_messages_received_total " + metrics.getTotalMessagesReceived() + "\n"
            + "# HELP actiongame_messages_sent_total Sent messages\n"
            + "# TYPE actiongame_messages_sent_total counter\n"
            + "actiongame_messages_sent_total " + metrics.getTotalMessagesSent() + "\n"
            + "# HELP actiongame_errors_total Server errors\n"
            + "# TYPE actiongame_errors_total counter\n"
            + "actiongame_errors_total " + metrics.getTotalErrors() + "\n"
            + "# HELP actiongame_frames_total Executed battle frames\n"
            + "# TYPE actiongame_frames_total counter\n"
            + "actiongame_frames_total " + metrics.getTotalFramesExecuted() + "\n"
            + "# HELP actiongame_actions_total Player actions\n"
            + "# TYPE actiongame_actions_total counter\n"
            + "actiongame_actions_total " + metrics.getTotalPlayerActions() + "\n"
            + "# HELP actiongame_logins_total Login attempts\n"
            + "# TYPE actiongame_logins_total counter\n"
            + "actiongame_logins_total{result=\"success\"} " + metrics.getLoginSuccess() + "\n"
            + "actiongame_logins_total{result=\"failure\"} " + metrics.getLoginFailures() + "\n"
            + "# HELP actiongame_cheats_total Cheat incidents\n"
            + "# TYPE actiongame_cheats_total counter\n"
            + "actiongame_cheats_total " + metrics.getCheatIncidents() + "\n";
    }

    private void writeJson(HttpExchange exchange, String body) throws IOException {
        writeJson(exchange, body, 200);
    }

    private void writeJson(HttpExchange exchange, String body, int status) throws IOException {
        write(exchange, "application/json", body, status);
    }

    private void writeText(HttpExchange exchange, String body) throws IOException {
        write(exchange, "text/plain; version=0.0.4", body);
    }

    private void write(HttpExchange exchange, String contentType, String body) throws IOException {
        write(exchange, contentType, body, 200);
    }

    private void write(HttpExchange exchange, String contentType, String body, int status) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
