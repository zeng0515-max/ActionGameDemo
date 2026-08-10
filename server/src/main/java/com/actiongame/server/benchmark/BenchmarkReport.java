package com.actiongame.server.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.ToLongFunction;

public class BenchmarkReport {
    private final LoadTestConfig config;
    private final List<LoadTestClientStats> results;
    private final long elapsedMs;
    private final double avgCpuLoad;

    public BenchmarkReport(LoadTestConfig config, List<LoadTestClientStats> results,
                           long elapsedMs, double avgCpuLoad) {
        this.config = config;
        this.results = results;
        this.elapsedMs = elapsedMs;
        this.avgCpuLoad = avgCpuLoad;
    }

    public String toMarkdown() {
        long totalActions = sumLong(LoadTestClientStats::getActionsSent);
        long totalRttCount = sumLong(LoadTestClientStats::getActionRttCount);
        long totalRttMs = sumLong(LoadTestClientStats::getActionRttTotalMs);
        long totalFrames = sumLong(LoadTestClientStats::getFramesReceived);
        long totalErrors = sumLong(LoadTestClientStats::getErrors);
        long totalBytesReceived = sumLong(LoadTestClientStats::getBytesReceived);
        long totalBytesSent = sumLong(LoadTestClientStats::getBytesSent);
        double avgRtt = totalRttCount == 0 ? 0 : (double) totalRttMs / totalRttCount;
        double seconds = Math.max(1, elapsedMs) / 1000.0;

        return "# Benchmark Report\n\n"
            + "- Date: " + LocalDateTime.now() + "\n"
            + "- Host: " + config.getHost() + ":" + config.getPort() + "\n"
            + "- Clients: " + config.getClientCount() + "\n"
            + "- Rooms: " + config.getRoomCount() + "\n"
            + "- Duration: " + config.getDurationMs() + " ms\n"
            + "- Action interval: " + config.getActionIntervalMs() + " ms\n"
            + "- Elapsed: " + elapsedMs + " ms\n"
            + "- Avg process CPU load: " + String.format("%.1f%%", avgCpuLoad * 100) + "\n\n"
            + "| Metric | Value |\n"
            + "|---|---|\n"
            + "| Actions sent | " + totalActions + " |\n"
            + "| Action responses | " + totalRttCount + " |\n"
            + "| Average action RTT | " + String.format("%.2f ms", avgRtt) + " |\n"
            + "| Frame notifications received | " + totalFrames + " |\n"
            + "| Frame notifications received / client | "
            + String.format("%.1f", results.isEmpty() ? 0 : (double) totalFrames / results.size()) + " |\n"
            + "| Frame notifications / second | " + String.format("%.1f", totalFrames / seconds) + " |\n"
            + "| Bytes received | " + totalBytesReceived + " |\n"
            + "| Bytes sent | " + totalBytesSent + " |\n"
            + "| Received rate | " + String.format("%.1f KB/s", totalBytesReceived / 1024.0 / seconds) + " |\n"
            + "| Sent rate | " + String.format("%.1f KB/s", totalBytesSent / 1024.0 / seconds) + " |\n"
            + "| Errors | " + totalErrors + " |\n";
    }

    public void writeTo(Path output) throws IOException {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output, toMarkdown());
    }

    private long sumLong(ToLongFunction<LoadTestClientStats> mapper) {
        return results.stream().mapToLong(mapper).sum();
    }
}
