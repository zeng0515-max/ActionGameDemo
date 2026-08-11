package com.actiongame.server.routing;

import com.actiongame.server.persistence.InMemoryStringStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("节点注册心跳")
class NodeRegistryHeartbeatTest {

    @Test
    @DisplayName("should_keepNodeAliveAndUnregisterOnStop")
    void should_keepNodeAliveAndUnregisterOnStop() throws InterruptedException {
        InMemoryStringStore store = new InMemoryStringStore();
        NodeRegistry registry = new NodeRegistry(store, "node-a", "game-a:9090", 1, 1);

        registry.start();
        Thread.sleep(1200);
        assertThat(registry.resolve("node-a")).isEqualTo("game-a:9090");

        registry.stop();
        assertThat(registry.resolve("node-a")).isEmpty();
    }
}
