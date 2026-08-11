package com.actiongame.server.routing;

import com.actiongame.server.persistence.StringStore;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NodeRegistry {
    private static final String KEY_PREFIX = "node:";

    private final StringStore store;
    private final String nodeId;
    private final String nodeAddress;
    private final int ttlSeconds;
    private final int refreshIntervalSeconds;
    private final ScheduledExecutorService scheduler;

    public NodeRegistry(StringStore store, String nodeId, String nodeAddress, int ttlSeconds) {
        this(store, nodeId, nodeAddress, ttlSeconds, Math.max(1, ttlSeconds / 3));
    }

    public NodeRegistry(StringStore store, String nodeId, String nodeAddress,
                        int ttlSeconds, int refreshIntervalSeconds) {
        this.store = store;
        this.nodeId = nodeId;
        this.nodeAddress = nodeAddress;
        this.ttlSeconds = ttlSeconds;
        this.refreshIntervalSeconds = refreshIntervalSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "node-registry-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void register() {
        if (nodeId != null && !nodeId.isBlank()) {
            store.set(KEY_PREFIX + nodeId, nodeAddress == null ? "" : nodeAddress, ttlSeconds);
        }
    }

    public void start() {
        register();
        scheduler.scheduleWithFixedDelay(
            this::register, refreshIntervalSeconds, refreshIntervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
        unregister();
    }

    public void unregister() {
        if (nodeId != null && !nodeId.isBlank()) {
            store.del(KEY_PREFIX + nodeId);
        }
    }

    public String resolve(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "";
        }
        String address = store.get(KEY_PREFIX + nodeId);
        return address == null ? "" : address;
    }

    public Collection<String> listNodeIds() {
        return store.keys(KEY_PREFIX + "*").stream()
            .map(key -> key.substring(KEY_PREFIX.length()))
            .toList();
    }

    public String getNodeId() {
        return nodeId;
    }
}
