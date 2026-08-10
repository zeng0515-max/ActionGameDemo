package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("告警规则")
class AlertEvaluatorTest {

    @Test
    @DisplayName("should_triggerAlerts_when_thresholdExceeded")
    void should_triggerAlerts_when_thresholdExceeded() {
        ServerMetrics metrics = new ServerMetrics();
        for (int i = 0; i < 11; i++) metrics.onError("test");
        for (int i = 0; i < 101; i++) metrics.onBattleStart();
        AlertEvaluator evaluator = new AlertEvaluator(10, 100, 1000);

        List<String> alerts = evaluator.evaluate(metrics);

        assertThat(alerts).anyMatch(a -> a.contains("error"));
        assertThat(alerts).anyMatch(a -> a.contains("rooms"));
    }

    @Test
    @DisplayName("should_triggerLoginFailureAlert_when_rateHigh")
    void should_triggerLoginFailureAlert_when_rateHigh() {
        ServerMetrics metrics = new ServerMetrics();
        metrics.onLoginFailure();
        metrics.onLoginFailure();
        metrics.onLoginSuccess();
        AlertEvaluator evaluator = new AlertEvaluator();

        List<String> alerts = evaluator.evaluate(metrics);

        assertThat(alerts).anyMatch(a -> a.contains("login"));
    }

    @Test
    @DisplayName("should_returnEmpty_when_healthy")
    void should_returnEmpty_when_healthy() {
        AlertEvaluator evaluator = new AlertEvaluator();

        assertThat(evaluator.evaluate(new ServerMetrics())).isEmpty();
    }
}
