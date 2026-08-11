package com.actiongame.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("配置加载测试")
class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    @Test
    @DisplayName("should_loadCharacterConfigs_when_readingFromClasspath")
    void should_loadCharacterConfigs_when_readingFromClasspath() {
        // Given
        String resourcePath = "config/characters.json";

        // When
        List<CharacterConfig> configs = loader.loadListFromClasspath(resourcePath, CharacterConfig.class);

        // Then
        assertThat(configs).hasSize(1);
        CharacterConfig fox = configs.get(0);
        assertThat(fox.getCharacterName()).isEqualTo("NineTailedFox");
        assertThat(fox.getMaxHealth()).isEqualTo(200f);
        assertThat(fox.getAttackPower()).isEqualTo(25f);
        assertThat(fox.getCriticalRate()).isEqualTo(0.15f);
    }

    @Test
    @DisplayName("should_loadMonsterConfigs_when_readingFromClasspath")
    void should_loadMonsterConfigs_when_readingFromClasspath() {
        // Given
        String resourcePath = "config/monsters.json";

        // When
        List<MonsterConfig> configs = loader.loadListFromClasspath(resourcePath, MonsterConfig.class);

        // Then
        assertThat(configs).hasSize(2);
        assertThat(configs.get(0).getEnemyName()).isEqualTo("Goblin");
        assertThat(configs.get(0).getMaxHealth()).isEqualTo(50f);
        assertThat(configs.get(1).getEnemyName()).isEqualTo("Boss");
        assertThat(configs.get(1).getMaxHealth()).isEqualTo(500f);
    }

    @Test
    @DisplayName("should_loadSkillConfigs_when_readingFromClasspath")
    void should_loadSkillConfigs_when_readingFromClasspath() {
        // Given
        String resourcePath = "config/skills.json";

        // When
        List<SkillConfig> configs = loader.loadListFromClasspath(resourcePath, SkillConfig.class);

        // Then
        assertThat(configs).hasSize(3);
        assertThat(configs.get(0).getSkillName()).isEqualTo("Normal Attack");
        assertThat(configs.get(0).getDamageMultiplier()).isEqualTo(1.0f);
        assertThat(configs.get(2).getSkillName()).isEqualTo("Ultimate Nova");
        assertThat(configs.get(2).getCooldown()).isEqualTo(30.0f);
    }

    @Test
    @DisplayName("should_loadBuffConfigs_when_readingFromClasspath")
    void should_loadBuffConfigs_when_readingFromClasspath() {
        // Given
        String resourcePath = "config/buffs.json";

        // When
        List<BuffConfig> configs = loader.loadListFromClasspath(resourcePath, BuffConfig.class);

        // Then
        assertThat(configs).hasSize(4);
        BuffConfig attackUp = configs.get(0);
        assertThat(attackUp.getBuffName()).isEqualTo("Attack Up");
        assertThat(attackUp.getBuffType()).isNotNull();
        assertThat(attackUp.getDuration()).isEqualTo(10f);

        BuffConfig shield = configs.get(3);
        assertThat(shield.getBuffName()).isEqualTo("Shield");
        assertThat(shield.getShieldValue()).isEqualTo(100f);
    }
}
