package com.actiongame.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("存储配置")
class StorageConfigTest {

    @Test
    @DisplayName("should_defaultToInMemory_when_noEnv")
    void should_defaultToInMemory_when_noEnv() {
        StorageConfig config = new StorageConfig(Map.of());

        assertThat(config.isMySqlEnabled()).isFalse();
        assertThat(config.isRedisEnabled()).isFalse();
    }

    @Test
    @DisplayName("should_enableStores_when_configured")
    void should_enableStores_when_configured() {
        StorageConfig config = new StorageConfig(Map.of(
            "MATCH_DB_URL", "jdbc:mysql://localhost:3306/actiongame",
            "REDIS_HOST", "localhost",
            "REDIS_PORT", "6379"
        ));

        assertThat(config.isMySqlEnabled()).isTrue();
        assertThat(config.getMatchDbUrl()).isEqualTo("jdbc:mysql://localhost:3306/actiongame");
        assertThat(config.isRedisEnabled()).isTrue();
        assertThat(config.getRedisPort()).isEqualTo(6379);
    }
}
