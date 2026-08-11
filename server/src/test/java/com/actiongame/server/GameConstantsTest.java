package com.actiongame.server;

import com.actiongame.server.constant.GameConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GameConstants 验证")
class GameConstantsTest {

    @Test
    @DisplayName("协议版本应在服务端支持范围内")
    void protocolVersion_shouldBeWithinServerRange() {
        assertThat(GameConstants.PROTOCOL_VERSION)
                .isGreaterThanOrEqualTo(GameConstants.SERVER_MIN_VERSION)
                .isLessThanOrEqualTo(GameConstants.SERVER_MAX_VERSION);
    }

    @Test
    @DisplayName("帧间隔应为20ms (50fps)")
    void frameInterval_shouldBe20ms() {
        assertThat(GameConstants.FRAME_INTERVAL_MS).isEqualTo(20);
    }

    @Test
    @DisplayName("WebSocket路径应为/game")
    void webSocketPath_shouldBeGame() {
        assertThat(GameConstants.WEBSOCKET_PATH).isEqualTo("/game");
    }

    @Test
    @DisplayName("心跳超时应大于0")
    void heartbeatTimeout_shouldBePositive() {
        assertThat(GameConstants.HEARTBEAT_TIMEOUT_MS).isPositive();
    }
}
