package com.actiongame.server.room;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("房间排空")
class RoomManagerDrainTest {

    @Test
    @DisplayName("should_allowExistingRoomButRejectNewRoom_whenDraining")
    void should_allowExistingRoomButRejectNewRoom_whenDraining() {
        RoomManager manager = new RoomManager();
        manager.createRoom("existing-room");

        manager.setDraining(true);

        assertThat(manager.isDraining()).isTrue();
        assertThat(manager.getOrCreateRoom("existing-room")).isNotNull();
        assertThat(manager.tryAcquireRoom("new-room")).isFalse();
        assertThatThrownBy(() -> manager.createRoom("new-room"))
            .isInstanceOf(IllegalStateException.class);

        manager.destroyRoom("existing-room");
    }
}
