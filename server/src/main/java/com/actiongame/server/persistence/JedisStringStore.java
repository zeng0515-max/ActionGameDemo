package com.actiongame.server.persistence;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collection;

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
    public boolean setIfAbsent(String key, String value, int ttlSeconds) {
        try (var jedis = pool.getResource()) {
            return "OK".equals(jedis.set(key, value, SetParams.setParams().nx().ex(ttlSeconds)));
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

    @Override
    public Collection<String> keys(String pattern) {
        try (var jedis = pool.getResource()) {
            return jedis.keys(pattern);
        }
    }
}
