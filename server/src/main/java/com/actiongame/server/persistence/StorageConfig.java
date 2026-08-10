package com.actiongame.server.persistence;

import java.util.Map;

public class StorageConfig {
    private final String matchDbUrl;
    private final String matchDbUser;
    private final String matchDbPassword;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;

    public StorageConfig(Map<String, String> env) {
        this.matchDbUrl = env.getOrDefault("MATCH_DB_URL", "");
        this.matchDbUser = env.getOrDefault("MATCH_DB_USER", "root");
        this.matchDbPassword = env.getOrDefault("MATCH_DB_PASSWORD", "");
        this.redisHost = env.getOrDefault("REDIS_HOST", "");
        this.redisPort = parsePort(env.getOrDefault("REDIS_PORT", "6379"));
        this.redisPassword = env.getOrDefault("REDIS_PASSWORD", "");
    }

    public static StorageConfig fromEnv() {
        return new StorageConfig(System.getenv());
    }

    public boolean isMySqlEnabled() {
        return !matchDbUrl.isEmpty();
    }

    public boolean isRedisEnabled() {
        return !redisHost.isEmpty();
    }

    public String getMatchDbUrl() { return matchDbUrl; }
    public String getMatchDbUser() { return matchDbUser; }
    public String getMatchDbPassword() { return matchDbPassword; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisPassword() { return redisPassword; }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 6379;
        }
    }
}
