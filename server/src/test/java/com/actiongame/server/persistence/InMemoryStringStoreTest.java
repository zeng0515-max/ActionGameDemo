package com.actiongame.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("内存字符串存储")
class InMemoryStringStoreTest {

    @Test
    @DisplayName("should_setIfAbsentOnlyOnce_whenKeyAlreadyExists")
    void should_setIfAbsentOnlyOnce_whenKeyAlreadyExists() {
        InMemoryStringStore store = new InMemoryStringStore();

        assertThat(store.setIfAbsent("room-owner", "node-a", 60)).isTrue();
        assertThat(store.setIfAbsent("room-owner", "node-b", 60)).isFalse();
        assertThat(store.get("room-owner")).isEqualTo("node-a");

        store.del("room-owner");
        assertThat(store.setIfAbsent("room-owner", "node-b", 60)).isTrue();
    }

    @Test
    @DisplayName("should_reacquireExpiredKey_whenOldOwnerExpired")
    void should_reacquireExpiredKey_whenOldOwnerExpired() throws InterruptedException {
        InMemoryStringStore store = new InMemoryStringStore();

        assertThat(store.setIfAbsent("room-owner", "node-a", 1)).isTrue();
        Thread.sleep(1100);

        assertThat(store.setIfAbsent("room-owner", "node-b", 60)).isTrue();
        assertThat(store.get("room-owner")).isEqualTo("node-b");
    }

    @Test
    @DisplayName("should_listKeysByPrefix")
    void should_listKeysByPrefix() {
        InMemoryStringStore store = new InMemoryStringStore();
        store.set("node:a", "a:9090", 60);
        store.set("node:b", "b:9090", 60);
        store.set("other", "x", 60);

        assertThat(store.keys("node:*")).containsExactlyInAnyOrder("node:a", "node:b");
    }
}
