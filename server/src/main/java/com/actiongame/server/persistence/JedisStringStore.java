package com.actiongame.server.persistence;

import redis.clients.jedis.JedisPool;

public class JedisStringStore implements StringStore {
    private final JedisPool pool;
    private final int ttlSeconds;

    public JedisStringStore(JedisPool pool, int ttlSeconds) {
        this.pool = pool;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public void set(String key, String value, int ttlSeconds) {
        try (var jedis = pool.getResource()) {
            jedis.setex(key, ttlSeconds, value);
        }
    }

    @Override
    public String get(String key) {
        try (var jedis = pool.getResource()) {
            return jedis.get(key);
        }
    }

    @Override
    public void del(String key) {
        try (var jedis = pool.getResource()) {
            jedis.del(key);
        }
    }
}
