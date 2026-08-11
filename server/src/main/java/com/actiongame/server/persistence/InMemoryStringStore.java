package com.actiongame.server.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryStringStore implements StringStore {
    private final Map<String, ExpiringValue> values = new ConcurrentHashMap<>();

    @Override
    public void set(String key, String value, int ttlSeconds) {
        values.put(key, new ExpiringValue(value, System.currentTimeMillis() + ttlSeconds * 1000L));
    }

    @Override
    public boolean setIfAbsent(String key, String value, int ttlSeconds) {
        long now = System.currentTimeMillis();
        values.compute(key, (k, existing) -> {
            if (existing != null && existing.expiresAt <= now) {
                return new ExpiringValue(value, now + ttlSeconds * 1000L);
            }
            if (existing == null) {
                return new ExpiringValue(value, now + ttlSeconds * 1000L);
            }
            return existing;
        });
        return value.equals(get(key));
    }

    @Override
    public String get(String key) {
        ExpiringValue existing = values.get(key);
        if (existing == null) {
            return null;
        }
        if (existing.expiresAt <= System.currentTimeMillis()) {
            values.remove(key, existing);
            return null;
        }
        return existing.value;
    }

    @Override
    public void del(String key) {
        values.remove(key);
    }

    @Override
    public Collection<String> keys(String pattern) {
        String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;
        long now = System.currentTimeMillis();
        Set<String> keys = values.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix))
            .filter(entry -> entry.getValue().expiresAt > now)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        return keys;
    }

    private static final class ExpiringValue {
        private final String value;
        private final long expiresAt;

        private ExpiringValue(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }
}
