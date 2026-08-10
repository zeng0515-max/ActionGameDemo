package com.actiongame.server.persistence;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryMatchResultRepository implements MatchResultRepository {
    private final Map<String, List<MatchResult>> byRoom = new ConcurrentHashMap<>();
    private final AtomicLong total = new AtomicLong();

    @Override
    public void save(MatchResult result) {
        byRoom.computeIfAbsent(result.getRoomId(), k -> new CopyOnWriteArrayList<>()).add(result);
        total.incrementAndGet();
    }

    @Override
    public List<MatchResult> findByRoomId(String roomId) {
        return List.copyOf(byRoom.getOrDefault(roomId, List.of()));
    }

    @Override
    public long count() {
        return total.get();
    }
}
