package com.actiongame.server.persistence;

import com.actiongame.server.room.BattleRoom;
import com.actiongame.server.room.RoomManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("房间生命周期持久化")
class RoomManagerPersistenceTest {

    @Test
    @DisplayName("should_cacheAndRemoveRoom_when_roomLifecycleManaged")
    void should_cacheAndRemoveRoom_when_roomLifecycleManaged() {
        InMemoryMatchResultRepository repository = new InMemoryMatchResultRepository();
        InMemoryRoomStateCache cache = new InMemoryRoomStateCache();
        RoomManager manager = new RoomManager(repository, cache);

        BattleRoom room = manager.createRoom("room-lifecycle-1");
        assertThat(cache.get("room-lifecycle-1")).isNotNull();

        manager.destroyRoom("room-lifecycle-1");
        assertThat(cache.get("room-lifecycle-1")).isNull();
    }
}
