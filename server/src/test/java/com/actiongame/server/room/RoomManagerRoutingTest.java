package com.actiongame.server.room;

import com.actiongame.server.persistence.InMemoryMatchResultRepository;
import com.actiongame.server.persistence.InMemoryRoomStateCache;
import com.actiongame.server.persistence.InMemoryStringStore;
import com.actiongame.server.persistence.StringStore;
import com.actiongame.server.routing.NodeRegistry;
import com.actiongame.server.routing.RoomRouter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("房间路由集成")
class RoomManagerRoutingTest {

    @Test
    @DisplayName("should_redirectToOwnerNode_whenRoomAlreadyOwned")
    void should_redirectToOwnerNode_whenRoomAlreadyOwned() {
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

        nodeA.createRoom("room-route-1");

        assertThat(nodeB.getRoomOwner("room-route-1")).isEqualTo("node-a");
        assertThat(nodeB.resolveNodeAddress("node-a")).isEqualTo("game-a:9090");
        assertThatThrownBy(() -> nodeB.getOrCreateRoom("room-route-1"))
            .isInstanceOf(IllegalStateException.class);

        nodeA.destroyRoom("room-route-1");
        assertThat(nodeB.getRoomOwner("room-route-1")).isNull();
        assertThat(nodeB.tryAcquireRoom("room-route-1")).isTrue();
    }

    @Test
    @DisplayName("should_releaseRoomOwnership_whenOwnerNodeIsNotAlive")
    void should_releaseRoomOwnership_whenOwnerNodeIsNotAlive() {
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
        nodeA.createRoom("stale-room");
        nodeA.getNodeRegistry().unregister();

        assertThat(nodeB.releaseStaleRoom("stale-room")).isTrue();
        assertThat(nodeB.getRoomOwner("stale-room")).isNull();
    }
}
