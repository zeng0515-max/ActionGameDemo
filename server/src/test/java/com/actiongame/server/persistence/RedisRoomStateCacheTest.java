package com.actiongame.server.persistence;

import com.actiongame.server.room.RoomSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Redis 房间状态缓存")
class RedisRoomStateCacheTest {

    @Test
    @DisplayName("should_putGetRemove_when_stringStoreBacked")
    void should_putGetRemove_when_stringStoreBacked() {
        FakeStringStore store = new FakeStringStore();
        RedisRoomStateCache cache = new RedisRoomStateCache(store);
        RoomState state = new RoomState("room-1", "WAITING", 0, 0, 0L, 1000L);

        cache.put(state.getRoomId(), state);

        RoomState loaded = cache.get("room-1");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getRoomId()).isEqualTo("room-1");
        assertThat(loaded.getStatus()).isEqualTo("WAITING");

        cache.remove("room-1");
        assertThat(cache.get("room-1")).isNull();
    }

    @Test
    @DisplayName("should_putGetRemoveSnapshot_when_stringStoreBacked")
    void should_putGetRemoveSnapshot_when_stringStoreBacked() {
        FakeStringStore store = new FakeStringStore();
        RedisRoomStateCache cache = new RedisRoomStateCache(store);
        RoomSnapshot snapshot = new RoomSnapshot(
            "room-snap-1", "BATTLE", 10L, 1000L, 5,
            List.of(), List.of());

        cache.putSnapshot(snapshot.roomId(), snapshot);

        RoomSnapshot loaded = cache.getSnapshot("room-snap-1");
        assertThat(loaded).isNotNull();
        assertThat(loaded.roomId()).isEqualTo("room-snap-1");
        assertThat(loaded.status()).isEqualTo("BATTLE");

        cache.removeSnapshot("room-snap-1");
        assertThat(cache.getSnapshot("room-snap-1")).isNull();
    }

    private static class FakeStringStore implements StringStore {
        private final Map<String, String> data = new HashMap<>();

        @Override
        public void set(String key, String value, int ttlSeconds) {
            data.put(key, value);
        }

        @Override
        public boolean setIfAbsent(String key, String value, int ttlSeconds) {
            return data.putIfAbsent(key, value) == null;
        }

        @Override
        public String get(String key) {
            return data.get(key);
        }

        @Override
        public void del(String key) {
            data.remove(key);
        }

        @Override
        public Collection<String> keys(String pattern) {
            String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
            return data.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .collect(Collectors.toSet());
        }
    }
}
