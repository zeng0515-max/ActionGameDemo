package com.actiongame.server.persistence;

import com.actiongame.server.room.RoomSnapshot;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRoomStateCache implements RoomStateCache {
    private final Map<String, RoomState> states = new ConcurrentHashMap<>();
    private final Map<String, RoomSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public void put(String roomId, RoomState state) {
        states.put(roomId, state);
    }

    @Override
    public RoomState get(String roomId) {
        return states.get(roomId);
    }

    @Override
    public void remove(String roomId) {
        states.remove(roomId);
    }

    @Override
    public void putSnapshot(String roomId, RoomSnapshot snapshot) {
        snapshots.put(roomId, snapshot);
    }

    @Override
    public RoomSnapshot getSnapshot(String roomId) {
        return snapshots.get(roomId);
    }

    @Override
    public void removeSnapshot(String roomId) {
        snapshots.remove(roomId);
    }
}
