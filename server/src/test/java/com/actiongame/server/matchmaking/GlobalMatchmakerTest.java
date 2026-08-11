package com.actiongame.server.matchmaking;

import com.actiongame.server.persistence.InMemoryMatchResultRepository;
import com.actiongame.server.persistence.InMemoryRoomStateCache;
import com.actiongame.server.persistence.InMemoryStringStore;
import com.actiongame.server.persistence.StringStore;
import com.actiongame.server.room.RoomManager;
import com.actiongame.server.routing.NodeRegistry;
import com.actiongame.server.routing.RoomRouter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("全局匹配器")
class GlobalMatchmakerTest {

    @Test
    @DisplayName("should_assignRoomToLeastLoadedNode")
    void should_assignRoomToLeastLoadedNode() {
        StringStore store = new InMemoryStringStore();
        RoomManager nodeA = new RoomManager(
            new InMemoryMatchResultRepository(),
            new InMemoryRoomStateCache(),
            new RoomRouter(store, "node-a"),
            new NodeRegistry(store, "node-a", "game-a:9090", 3600));
        RoomManager nodeB = new RoomManager(
            new InMemoryMatchResultRepository(),
            new InMemoryRoomStateCache(),
            new RoomRouter(store, "node-b"),
            new NodeRegistry(store, "node-b", "game-b:9090", 3600));
        nodeA.createRoom("existing-room");

        GlobalMatchmaker matchmaker = new GlobalMatchmaker(
            nodeB, nodeB.getNodeRegistry(), nodeB.getRoomRouter());
        String roomId = matchmaker.allocateRoom("p1");

        assertThat(roomId).startsWith("match-");
        assertThat(nodeB.getRoomOwner(roomId)).isEqualTo("node-b");

        nodeA.destroyRoom("existing-room");
        nodeB.destroyRoom(roomId);
    }
}
