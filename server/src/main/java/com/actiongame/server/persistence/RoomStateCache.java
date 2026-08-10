package com.actiongame.server.persistence;

public interface RoomStateCache {
    void put(String roomId, RoomState state);
    RoomState get(String roomId);
    void remove(String roomId);
}
