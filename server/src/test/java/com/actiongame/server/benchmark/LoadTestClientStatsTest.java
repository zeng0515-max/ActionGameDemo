package com.actiongame.server.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("压测客户端统计")
class LoadTestClientStatsTest {

    @Test
    @DisplayName("should_averageRtt_when_rttRecorded")
    void should_averageRtt_when_rttRecorded() {
        LoadTestClientStats stats = new LoadTestClientStats("bench-0");

        stats.recordActionRtt(2);
        stats.recordActionRtt(6);

        assertThat(stats.getAverageActionRttMs()).isEqualTo(4.0);
        assertThat(stats.getActionRttCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_countFramesActionsAndBytes_when_recorded")
    void should_countFramesActionsAndBytes_when_recorded() {
        LoadTestClientStats stats = new LoadTestClientStats("bench-0");

        stats.recordFrame();
        stats.recordFrame();
        stats.recordActionSent();
        stats.recordBytesReceived(100);
        stats.recordBytesSent(50);
        stats.recordError();

        assertThat(stats.getFramesReceived()).isEqualTo(2);
        assertThat(stats.getActionsSent()).isEqualTo(1);
        assertThat(stats.getBytesReceived()).isEqualTo(100);
        assertThat(stats.getBytesSent()).isEqualTo(50);
        assertThat(stats.getErrors()).isEqualTo(1);
    }
}
