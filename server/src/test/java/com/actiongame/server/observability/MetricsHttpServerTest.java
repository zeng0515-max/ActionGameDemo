package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import com.actiongame.server.room.RoomManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("指标 HTTP 服务")
class MetricsHttpServerTest {

    @Test
    @DisplayName("should_exposeHealthMetricsAndAlerts")
    void should_exposeHealthMetricsAndAlerts() throws Exception {
        ServerMetrics metrics = new ServerMetrics();
        metrics.onConnect();
        metrics.onMessageReceived();
        MetricsHttpServer server = new MetricsHttpServer(0, metrics, new AlertEvaluator());
        server.start();
        try {
            String health = get(server.getPort(), "/health");
            assertThat(health).contains("\"status\":\"ok\"");

            String prometheus = get(server.getPort(), "/metrics");
            assertThat(prometheus).contains("actiongame_connections_active");
            assertThat(prometheus).contains("actiongame_messages_received_total");

            String alerts = get(server.getPort(), "/alerts");
            assertThat(alerts).contains("\"alerts\"");
        } finally {
            server.stop();
        }
    }

    @Test
    @DisplayName("should_exposeRoomRoutingWhenRoomManagerProvided")
    void should_exposeRoomRoutingWhenRoomManagerProvided() throws Exception {
        RoomManager roomManager = new RoomManager();
        roomManager.createRoom("route-http-1");
        MetricsHttpServer server = new MetricsHttpServer(0, new ServerMetrics(),
            new AlertEvaluator(), roomManager);
        server.start();
        try {
            String routing = get(server.getPort(), "/routing/room/route-http-1");
            assertThat(routing).contains("\"roomId\":\"route-http-1\"");
            assertThat(routing).contains("\"nodeId\":\"local\"");
            assertThat(routing).contains("\"address\":\"localhost:9090\"");
            assertThat(routing).contains("\"status\":\"assigned\"");

            String unassigned = get(server.getPort(), "/routing/room/missing-route");
            assertThat(unassigned).contains("\"status\":\"unassigned\"");
        } finally {
            server.stop();
            roomManager.destroyRoom("route-http-1");
        }
    }

    @Test
    @DisplayName("should_exposeDrainAndReadyWhenRoomManagerProvided")
    void should_exposeDrainAndReadyWhenRoomManagerProvided() throws Exception {
        RoomManager roomManager = new RoomManager();
        MetricsHttpServer server = new MetricsHttpServer(0, new ServerMetrics(),
            new AlertEvaluator(), roomManager);
        server.start();
        try {
            assertThat(get(server.getPort(), "/ready")).contains("\"status\":\"ready\"");

            String drain = post(server.getPort(), "/admin/drain");
            assertThat(drain).contains("\"status\":\"draining\"");
            assertThat(roomManager.isDraining()).isTrue();
            assertThat(get(server.getPort(), "/admin/status"))
                .contains("\"draining\":true")
                .contains("\"activeRooms\":0");
            assertThat(status(server.getPort(), "/ready")).isEqualTo(503);

            String undrain = post(server.getPort(), "/admin/undrain");
            assertThat(undrain).contains("\"status\":\"ready\"");
            assertThat(roomManager.isDraining()).isFalse();
        } finally {
            server.stop();
        }
    }

    private String get(int port, String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(
            "http://127.0.0.1:" + port + path).toURL().openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().reduce("", (a, b) -> a + b);
        } finally {
            connection.disconnect();
        }
    }

    private String post(int port, String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(
            "http://127.0.0.1:" + port + path).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.getOutputStream().close();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().reduce("", (a, b) -> a + b);
        } finally {
            connection.disconnect();
        }
    }

    private int status(int port, String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(
            "http://127.0.0.1:" + port + path).toURL().openConnection();
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }
}
