package com.actiongame.server.routing;

import com.actiongame.server.persistence.InMemoryStringStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("房间路由")
class RoomRouterTest {

    @Test
    @DisplayName("should_keepFirstOwner_whenTwoNodesRaceForSameRoom")
    void should_keepFirstOwner_whenTwoNodesRaceForSameRoom() {
        InMemoryStringStore store = new InMemoryStringStore();
        RoomRouter nodeA = new RoomRouter(store, "node-a");
        RoomRouter nodeB = new RoomRouter(store, "node-b");

        assertThat(nodeA.tryAcquire("room-1")).isTrue();
        assertThat(nodeA.getOwner("room-1")).isEqualTo("node-a");
        assertThat(nodeB.tryAcquire("room-1")).isFalse();
        assertThat(nodeB.getOwner("room-1")).isEqualTo("node-a");

        assertThat(nodeA.releaseIfOwner("room-1")).isTrue();
        assertThat(nodeB.tryAcquire("room-1")).isTrue();
        assertThat(nodeB.getOwner("room-1")).isEqualTo("node-b");
    }

    @Test
    @DisplayName("should_assignRoomToChosenNodeAndCountRooms")
    void should_assignRoomToChosenNodeAndCountRooms() {
        InMemoryStringStore store = new InMemoryStringStore();
        RoomRouter nodeA = new RoomRouter(store, "node-a");
        RoomRouter nodeB = new RoomRouter(store, "node-b");

        assertThat(nodeA.assignIfAbsent("room-2", "node-b")).isTrue();
        assertThat(nodeA.getOwner("room-2")).isEqualTo("node-b");
        assertThat(nodeA.countRoomsOwnedBy("node-a")).isZero();
        assertThat(nodeA.countRoomsOwnedBy("node-b")).isEqualTo(1);
    }
}
