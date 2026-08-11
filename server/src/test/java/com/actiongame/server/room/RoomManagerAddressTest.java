package com.actiongame.server.room;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("节点地址解析")
class RoomManagerAddressTest {

    @Test
    @DisplayName("should_preferExplicitAddressOverTemplate")
    void should_preferExplicitAddressOverTemplate() {
        assertThat(RoomManager.resolveNodeAddress(
            "game-server-0", "game.example:9090", "${NODE_ID}.headless:9090"))
            .isEqualTo("game.example:9090");
    }

    @Test
    @DisplayName("should_expandTemplateWhenExplicitAddressMissing")
    void should_expandTemplateWhenExplicitAddressMissing() {
        assertThat(RoomManager.resolveNodeAddress(
            "game-server-0", null, "${NODE_ID}.game-server-headless.actiongame.svc.cluster.local:9090"))
            .isEqualTo("game-server-0.game-server-headless.actiongame.svc.cluster.local:9090");
    }

    @Test
    @DisplayName("should_fallBackToLocalhostWhenNoAddressConfigured")
    void should_fallBackToLocalhostWhenNoAddressConfigured() {
        assertThat(RoomManager.resolveNodeAddress("game-server-0", null, null))
            .isEqualTo("localhost:9090");
    }
}
