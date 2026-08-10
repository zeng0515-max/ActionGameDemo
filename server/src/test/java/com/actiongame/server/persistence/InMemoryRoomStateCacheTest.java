package com.actiongame.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("内存房间状态缓存")
class InMemoryRoomStateCacheTest {

    @Test
    @DisplayName("should_putGetRemove_when_stateManaged")
    void should_putGetRemove_when_stateManaged() {
        InMemoryRoomStateCache cache = new InMemoryRoomStateCache();
        RoomState state = new RoomState("room-1", "BATTLE", 2, 3, 100L, 5000L);

        cache.put(state.getRoomId(), state);

        RoomState loaded = cache.get("room-1");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getStatus()).isEqualTo("BATTLE");
        assertThat(loaded.getPlayerCount()).isEqualTo(2);

        cache.remove("room-1");
        assertThat(cache.get("room-1")).isNull();
    }
}
