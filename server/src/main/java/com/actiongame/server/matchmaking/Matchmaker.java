package com.actiongame.server.matchmaking;

public interface Matchmaker {
    String allocateRoom(String playerId);
}
