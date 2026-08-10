package com.actiongame.server.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class StorageFactory {
    private static final int REDIS_TTL_SECONDS = 1800;

    private StorageFactory() {}

    public static MatchResultRepository createMatchResultRepository(StorageConfig config) {
        if (config.isMySqlEnabled()) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getMatchDbUrl());
            hikariConfig.setUsername(config.getMatchDbUser());
            hikariConfig.setPassword(config.getMatchDbPassword());
            hikariConfig.setMaximumPoolSize(10);
            return new JdbcMatchResultRepository(new HikariDataSource(hikariConfig));
        }
        return new InMemoryMatchResultRepository();
    }

    public static RoomStateCache createRoomStateCache(StorageConfig config) {
        if (config.isRedisEnabled()) {
            JedisPool pool;
            if (config.getRedisPassword().isEmpty()) {
                pool = new JedisPool(config.getRedisHost(), config.getRedisPort());
            } else {
                pool = new JedisPool(new JedisPoolConfig(), config.getRedisHost(),
                    config.getRedisPort(), 2000, config.getRedisPassword());
            }
            return new RedisRoomStateCache(new JedisStringStore(pool, REDIS_TTL_SECONDS));
        }
        return new InMemoryRoomStateCache();
    }
}
