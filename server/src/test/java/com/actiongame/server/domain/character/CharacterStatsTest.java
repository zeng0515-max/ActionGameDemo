package com.actiongame.server.domain.character;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("角色属性测试")
class CharacterStatsTest {

    @Test
    @DisplayName("should_returnCorrectEffectiveAttackPower_when_attackModifierAdded")
    void should_returnCorrectEffectiveAttackPower_when_attackModifierAdded() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);

        // When
        stats.addAttackModifier(0.2f); // +20% attack

        // Then
        assertThat(stats.getEffectiveAttackPower()).isEqualTo(24f, within(0.001f));
    }

    @Test
    @DisplayName("should_clampHealthToZero_when_takingLethalDamage")
    void should_clampHealthToZero_when_takingLethalDamage() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);

        // When
        stats.modifyHealth(-200f);

        // Then
        assertThat(stats.getCurrentHealth()).isEqualTo(0f);
        assertThat(stats.isDead()).isTrue();
    }

    @Test
    @DisplayName("should_clampHealthToMax_when_overhealing")
    void should_clampHealthToMax_when_overhealing() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        stats.modifyHealth(-50f);

        // When
        stats.modifyHealth(200f);

        // Then
        assertThat(stats.getCurrentHealth()).isEqualTo(100f);
        assertThat(stats.isFullHealth()).isTrue();
    }

    @Test
    @DisplayName("should_resetAllModifiers_when_resetCalled")
    void should_resetAllModifiers_when_resetCalled() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        stats.addAttackModifier(0.5f);
        stats.addDefenseModifier(0.3f);
        stats.addMoveSpeedModifier(-0.5f);
        stats.setDamageTakenMultiplier(1.25f);

        // When
        stats.resetAllModifiers();

        // Then
        assertThat(stats.getEffectiveAttackPower()).isEqualTo(10f, within(0.001f));
        assertThat(stats.getEffectiveDefense()).isEqualTo(5f, within(0.001f));
        assertThat(stats.getEffectiveMoveSpeed()).isEqualTo(5f, within(0.001f));
        assertThat(stats.getDamageTakenMultiplier()).isEqualTo(1f);
    }

    @Test
    @DisplayName("should_clampCriticalRateTo01_when_modifierExceedsRange")
    void should_clampCriticalRateTo01_when_modifierExceedsRange() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 10f, 5f, 5f, 0.5f, 1.5f);

        // When
        stats.addCriticalRateModifier(0.8f); // 0.5 + 0.8 = 1.3, should clamp to 1.0

        // Then
        assertThat(stats.getEffectiveCriticalRate()).isEqualTo(1.0f);
    }

    @Test
    @DisplayName("should_causeDeath_when_characterTakesDamageAtZeroHealth")
    void should_causeDeath_when_characterTakesDamageAtZeroHealth() {
        // Given
        CharacterStats stats = new CharacterStats(50f, 10f, 0f, 5f, 0.1f, 1.5f);

        // When
        stats.modifyHealth(-50f);

        // Then
        assertThat(stats.getCurrentHealth()).isEqualTo(0f);
        assertThat(stats.isDead()).isTrue();
    }
}
