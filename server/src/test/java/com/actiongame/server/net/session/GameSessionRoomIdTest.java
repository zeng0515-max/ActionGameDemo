package com.actiongame.server.net.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameSession 房间归属")
class GameSessionRoomIdTest {

    @Test
    @DisplayName("should_returnNull_when_noRoomJoined")
    void should_returnNull_when_noRoomJoined() {
        GameSession session = new GameSession("p1", null);

        assertThat(session.getRoomId()).isNull();
    }

    @Test
    @DisplayName("should_setAndGetRoomId_when_roomJoined")
    void should_setAndGetRoomId_when_roomJoined() {
        GameSession session = new GameSession("p1", null);

        session.setRoomId("room-1");

        assertThat(session.getRoomId()).isEqualTo("room-1");
    }
}
