package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;

import java.util.ArrayList;
import java.util.List;

public class AlertEvaluator {
    private final long maxErrors;
    private final long maxActiveRooms;
    private final long maxConnections;

    public AlertEvaluator() {
        this(10, 100, 1000);
    }

    public AlertEvaluator(long maxErrors, long maxActiveRooms, long maxConnections) {
        this.maxErrors = maxErrors;
        this.maxActiveRooms = maxActiveRooms;
        this.maxConnections = maxConnections;
    }

    public List<String> evaluate(ServerMetrics metrics) {
        List<String> alerts = new ArrayList<>();
        if (metrics.getTotalErrors() > maxErrors) {
            alerts.add("High error count: " + metrics.getTotalErrors() + " > " + maxErrors);
        }
        if (metrics.getActiveRooms() > maxActiveRooms) {
            alerts.add("High active rooms: " + metrics.getActiveRooms() + " > " + maxActiveRooms);
        }
        if (metrics.getCurrentConnections() > maxConnections) {
            alerts.add("High connections: " + metrics.getCurrentConnections() + " > " + maxConnections);
        }
        long totalLogins = metrics.getLoginSuccess() + metrics.getLoginFailures();
        if (totalLogins > 0 && metrics.getLoginFailures() * 100 / totalLogins > 50) {
            alerts.add("High login failure rate: " + metrics.getLoginFailures() + "/" + totalLogins);
        }
        return alerts;
    }
}
