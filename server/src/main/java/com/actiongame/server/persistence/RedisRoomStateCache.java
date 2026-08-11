package com.actiongame.server.persistence;

import com.google.gson.Gson;
import com.actiongame.server.room.RoomSnapshot;

public class RedisRoomStateCache implements RoomStateCache {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_TTL_SECONDS = 1800;
    private static final String KEY_PREFIX = "room:";
    private static final String SNAPSHOT_KEY_PREFIX = "snapshot:";

    private final StringStore store;

    public RedisRoomStateCache(StringStore store) {
        this.store = store;
    }

    @Override
    public void put(String roomId, RoomState state) {
        store.set(KEY_PREFIX + roomId, GSON.toJson(state), DEFAULT_TTL_SECONDS);
    }

    @Override
    public RoomState get(String roomId) {
        String json = store.get(KEY_PREFIX + roomId);
        return json == null ? null : GSON.fromJson(json, RoomState.class);
    }

    @Override
    public void remove(String roomId) {
        store.del(KEY_PREFIX + roomId);
    }

    @Override
    public void putSnapshot(String roomId, RoomSnapshot snapshot) {
        store.set(SNAPSHOT_KEY_PREFIX + roomId, GSON.toJson(snapshot), DEFAULT_TTL_SECONDS);
    }

    @Override
    public RoomSnapshot getSnapshot(String roomId) {
        String json = store.get(SNAPSHOT_KEY_PREFIX + roomId);
        return json == null ? null : GSON.fromJson(json, RoomSnapshot.class);
    }

    @Override
    public void removeSnapshot(String roomId) {
        store.del(SNAPSHOT_KEY_PREFIX + roomId);
    }
}
