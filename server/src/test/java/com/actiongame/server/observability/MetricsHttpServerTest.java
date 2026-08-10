package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
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
}
