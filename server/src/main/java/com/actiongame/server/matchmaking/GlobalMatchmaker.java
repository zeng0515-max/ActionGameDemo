package com.actiongame.server.matchmaking;

import com.actiongame.server.room.RoomManager;
import com.actiongame.server.routing.NodeRegistry;
import com.actiongame.server.routing.RoomRouter;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

public class GlobalMatchmaker implements Matchmaker {
    private final RoomManager roomManager;
    private final NodeRegistry nodeRegistry;
    private final RoomRouter roomRouter;
    private final AtomicLong sequence = new AtomicLong();

    public GlobalMatchmaker(RoomManager roomManager, NodeRegistry nodeRegistry, RoomRouter roomRouter) {
        this.roomManager = roomManager;
        this.nodeRegistry = nodeRegistry;
        this.roomRouter = roomRouter;
    }

    @Override
    public String allocateRoom(String playerId) {
        String chosenNode = nodeRegistry.listNodeIds().stream()
            .min(Comparator.comparingInt(roomRouter::countRoomsOwnedBy))
            .orElse(roomManager.getNodeId());
        String roomId = "match-" + sequence.incrementAndGet();
        roomRouter.assignIfAbsent(roomId, chosenNode);
        return roomId;
    }
}
