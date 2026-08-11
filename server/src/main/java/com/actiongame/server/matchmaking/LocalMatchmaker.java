package com.actiongame.server.matchmaking;

import com.actiongame.server.room.BattleRoom;
import com.actiongame.server.room.RoomManager;
import com.actiongame.server.room.RoomStatus;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

public class LocalMatchmaker implements Matchmaker {
    private static final int DEFAULT_MAX_PLAYERS = 4;

    private final RoomManager roomManager;
    private final int maxPlayers;
    private final AtomicLong sequence = new AtomicLong();

    public LocalMatchmaker(RoomManager roomManager) {
        this(roomManager, DEFAULT_MAX_PLAYERS);
    }

    public LocalMatchmaker(RoomManager roomManager, int maxPlayers) {
        this.roomManager = roomManager;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public String allocateRoom(String playerId) {
        return roomManager.getAllRooms().stream()
            .filter(room -> room.getStatus() == RoomStatus.WAITING || room.getStatus() == RoomStatus.BATTLE)
            .filter(room -> room.getPlayerCount() < maxPlayers)
            .min(Comparator.comparingInt(BattleRoom::getPlayerCount))
            .map(BattleRoom::getRoomId)
            .orElseGet(() -> {
                String roomId = "match-" + sequence.incrementAndGet();
                roomManager.createRoom(roomId);
                return roomId;
            });
    }
}
