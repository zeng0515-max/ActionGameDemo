package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MetricsHttpServer {
    private static final Gson GSON = new Gson();

    private final HttpServer server;
    private final ServerMetrics metrics;
    private final AlertEvaluator alertEvaluator;

    public MetricsHttpServer(int port, ServerMetrics metrics, AlertEvaluator alertEvaluator) throws IOException {
        this.metrics = metrics;
        this.alertEvaluator = alertEvaluator;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext("/metrics", this::handleMetrics);
        server.createContext("/alerts", this::handleAlerts);
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

    private void handleMetrics(HttpExchange exchange) throws IOException {
        writeText(exchange, prometheusText());
    }

    private void handleAlerts(HttpExchange exchange) throws IOException {
        writeJson(exchange, GSON.toJson(Map.of("alerts", alertEvaluator.evaluate(metrics))));
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
        write(exchange, "application/json", body);
    }

    private void writeText(HttpExchange exchange, String body) throws IOException {
        write(exchange, "text/plain; version=0.0.4", body);
    }

    private void write(HttpExchange exchange, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
