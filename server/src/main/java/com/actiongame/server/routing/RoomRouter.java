package com.actiongame.server.routing;

import com.actiongame.server.persistence.StringStore;

public class RoomRouter {
    private static final String KEY_PREFIX = "room:owner:";
    private static final int DEFAULT_TTL_SECONDS = 3600;

    private final StringStore store;
    private final String nodeId;
    private final int ttlSeconds;

    public RoomRouter(StringStore store, String nodeId) {
        this(store, nodeId, DEFAULT_TTL_SECONDS);
    }

    public RoomRouter(StringStore store, String nodeId, int ttlSeconds) {
        this.store = store;
        this.nodeId = nodeId;
        this.ttlSeconds = ttlSeconds;
    }

    public boolean tryAcquire(String roomId) {
        String existingOwner = getOwner(roomId);
        if (existingOwner != null) {
            return nodeId.equals(existingOwner);
        }
        if (store.setIfAbsent(KEY_PREFIX + roomId, nodeId, ttlSeconds)) {
            return true;
        }
        return nodeId.equals(getOwner(roomId));
    }

    public boolean assignIfAbsent(String roomId, String ownerNodeId) {
        if (store.setIfAbsent(KEY_PREFIX + roomId, ownerNodeId, ttlSeconds)) {
            return true;
        }
        return ownerNodeId.equals(getOwner(roomId));
    }

    public String getOwner(String roomId) {
        return store.get(KEY_PREFIX + roomId);
    }

    public boolean releaseIfOwner(String roomId) {
        if (!nodeId.equals(getOwner(roomId))) {
            return false;
        }
        store.del(KEY_PREFIX + roomId);
        return true;
    }

    public void forceRelease(String roomId) {
        store.del(KEY_PREFIX + roomId);
    }

    public int countRoomsOwnedBy(String nodeId) {
        return (int) store.keys(KEY_PREFIX + "*").stream()
            .map(store::get)
            .filter(nodeId::equals)
            .count();
    }

    public String getNodeId() {
        return nodeId;
    }
}
