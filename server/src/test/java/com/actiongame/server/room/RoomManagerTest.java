package com.actiongame.server.room;

import com.actiongame.server.config.MonsterConfig;
import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.PlayerCharacter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("房间管理器测试")
class RoomManagerTest {

    @Test
    @DisplayName("should_createRoom_when_createCalled")
    void should_createRoom_when_createCalled() {
        // Given
        RoomManager manager = RoomManager.getInstance();

        // When
        BattleRoom room = manager.createRoom("test-room-01");

        // Then
        assertThat(room).isNotNull();
        assertThat(room.getRoomId()).isEqualTo("test-room-01");
        assertThat(manager.getRoom("test-room-01")).isSameAs(room);
        assertThat(manager.getRoomCount()).isGreaterThanOrEqualTo(1);

        // Cleanup
        manager.destroyRoom("test-room-01");
    }

    @Test
    @DisplayName("should_returnSameRoom_when_getOrCreateCalledTwice")
    void should_returnSameRoom_when_getOrCreateCalledTwice() {
        // Given
        RoomManager manager = RoomManager.getInstance();

        // When
        BattleRoom room1 = manager.getOrCreateRoom("test-room-02");
        BattleRoom room2 = manager.getOrCreateRoom("test-room-02");

        // Then
        assertThat(room2).isSameAs(room1);

        // Cleanup
        manager.destroyRoom("test-room-02");
    }

    @Test
    @DisplayName("should_destroyRoom_when_destroyCalled")
    void should_destroyRoom_when_destroyCalled() {
        // Given
        RoomManager manager = RoomManager.getInstance();
        manager.createRoom("test-room-03");
        assertThat(manager.getRoom("test-room-03")).isNotNull();

        // When
        manager.destroyRoom("test-room-03");

        // Then
        assertThat(manager.getRoom("test-room-03")).isNull();
    }
}
