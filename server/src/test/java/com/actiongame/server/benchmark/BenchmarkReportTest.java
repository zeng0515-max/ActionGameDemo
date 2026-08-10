package com.actiongame.server.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Benchmark 报告")
class BenchmarkReportTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should_writeMarkdown_when_reportCreated")
    void should_writeMarkdown_when_reportCreated() throws Exception {
        LoadTestConfig config = new LoadTestConfig(4, 1000L, 50, 4, "127.0.0.1", 9090);
        LoadTestClientStats stats = new LoadTestClientStats("bench-0");
        stats.recordFrame();
        stats.recordActionSent();
        stats.recordActionRtt(3);
        stats.recordBytesReceived(1024);
        stats.recordBytesSent(256);
        BenchmarkReport report = new BenchmarkReport(config, List.of(stats), 1000L, 0.12);

        Path file = tempDir.resolve("benchmark.md");
        report.writeTo(file);

        String content = Files.readString(file);
        assertThat(content).contains("# Benchmark Report");
        assertThat(content).contains("Average action RTT");
        assertThat(content).contains("Avg process CPU load");
    }
}
