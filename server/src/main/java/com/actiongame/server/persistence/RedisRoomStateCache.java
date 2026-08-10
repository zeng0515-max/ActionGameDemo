package com.actiongame.server.persistence;

import com.google.gson.Gson;

public class RedisRoomStateCache implements RoomStateCache {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_TTL_SECONDS = 1800;
    private static final String KEY_PREFIX = "room:";

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
}
