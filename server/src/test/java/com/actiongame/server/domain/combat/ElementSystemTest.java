package com.actiongame.server.domain.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("元素克制系统测试")
class ElementSystemTest {

    @Test
    @DisplayName("should_returnCounterMultiplier_when_fireAttacksIce")
    void should_returnCounterMultiplier_when_fireAttacksIce() {
        // Given
        ElementType attack = ElementType.FIRE;
        ElementType defender = ElementType.ICE;

        // When
        float multiplier = DamageInfo.getMultiplier(attack, defender);

        // Then
        assertThat(multiplier).isEqualTo(1.5f);
    }

    @Test
    @DisplayName("should_returnCounterMultiplier_when_iceAttacksLightning")
    void should_returnCounterMultiplier_when_iceAttacksLightning() {
        // Given
        ElementType attack = ElementType.ICE;
        ElementType defender = ElementType.LIGHTNING;

        // When
        float multiplier = DamageInfo.getMultiplier(attack, defender);

        // Then
        assertThat(multiplier).isEqualTo(1.5f);
    }

    @Test
    @DisplayName("should_returnCounterMultiplier_when_lightningAttacksWater")
    void should_returnCounterMultiplier_when_lightningAttacksWater() {
        // Given
        ElementType attack = ElementType.LIGHTNING;
        ElementType defender = ElementType.WATER;

        // When
        float multiplier = DamageInfo.getMultiplier(attack, defender);

        // Then
        assertThat(multiplier).isEqualTo(1.5f);
    }

    @Test
    @DisplayName("should_returnCounterMultiplier_when_waterAttacksFire")
    void should_returnCounterMultiplier_when_waterAttacksFire() {
        // Given
        ElementType attack = ElementType.WATER;
        ElementType defender = ElementType.FIRE;

        // When
        float multiplier = DamageInfo.getMultiplier(attack, defender);

        // Then
        assertThat(multiplier).isEqualTo(1.5f);
    }

    @Test
    @DisplayName("should_returnNormalMultiplier_when_noElementalAdvantage")
    void should_returnNormalMultiplier_when_noElementalAdvantage() {
        // Given
        ElementType attack = ElementType.FIRE;
        ElementType defender = ElementType.WATER; // water counters fire, but fire doesn't counter water

        // When
        float multiplier = DamageInfo.getMultiplier(attack, defender);

        // Then
        assertThat(multiplier).isEqualTo(1.0f);
    }

    @Test
    @DisplayName("should_returnNormalMultiplier_when_elementIsNone")
    void should_returnNormalMultiplier_when_elementIsNone() {
        // Given
        ElementType attack = ElementType.NONE;
        ElementType defender = ElementType.FIRE;

        // When
        float multiplier = DamageInfo.getMultiplier(attack, defender);

        // Then
        assertThat(multiplier).isEqualTo(1.0f);
    }

    @Test
    @DisplayName("should_returnTrue_when_isCounteringWithValidPair")
    void should_returnTrue_when_isCounteringWithValidPair() {
        // Given - all 4 counter relationships
        // When & Then
        assertThat(DamageInfo.isCountering(ElementType.FIRE, ElementType.ICE)).isTrue();
        assertThat(DamageInfo.isCountering(ElementType.ICE, ElementType.LIGHTNING)).isTrue();
        assertThat(DamageInfo.isCountering(ElementType.LIGHTNING, ElementType.WATER)).isTrue();
        assertThat(DamageInfo.isCountering(ElementType.WATER, ElementType.FIRE)).isTrue();
    }

    @Test
    @DisplayName("should_returnFalse_when_isCounteringWithSameElement")
    void should_returnFalse_when_isCounteringWithSameElement() {
        // Given
        ElementType attack = ElementType.FIRE;
        ElementType defender = ElementType.FIRE;

        // When
        boolean result = DamageInfo.isCountering(attack, defender);

        // Then
        assertThat(result).isFalse();
    }
}
