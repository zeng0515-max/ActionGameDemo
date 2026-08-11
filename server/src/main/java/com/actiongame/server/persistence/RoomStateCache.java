package com.actiongame.server.persistence;

import com.actiongame.server.room.RoomSnapshot;

public interface RoomStateCache {
    void put(String roomId, RoomState state);
    RoomState get(String roomId);
    void remove(String roomId);
    void putSnapshot(String roomId, RoomSnapshot snapshot);
    RoomSnapshot getSnapshot(String roomId);
    void removeSnapshot(String roomId);
}
