package com.actiongame.server.battle.combatsystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("连击系统测试")
class ComboSystemTest {

    @Test
    @DisplayName("should_incrementHitCount_when_registerHit")
    void should_incrementHitCount_when_registerHit() {
        // Given
        ComboSystem combo = new ComboSystem(4, 1.0f);

        // When
        combo.tryAdvanceCombo(1000);
        int hit1 = combo.registerHit(1050);
        int hit2 = combo.registerHit(1100);

        // Then
        assertThat(hit1).isEqualTo(1);
        assertThat(hit2).isEqualTo(2);
        assertThat(combo.getHitCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_resetCombo_when_timeoutReached")
    void should_resetCombo_when_timeoutReached() {
        // Given
        ComboSystem combo = new ComboSystem(4, 1.0f);
        combo.tryAdvanceCombo(1000);
        combo.registerHit(1050);

        // When: 超过1秒
        combo.update(2100);

        // Then
        assertThat(combo.getHitCount()).isEqualTo(0);
        assertThat(combo.getCurrentComboStep()).isEqualTo(0);
    }

    @Test
    @DisplayName("should_cycleComboStep_when_exceedingMaxStep")
    void should_cycleComboStep_when_exceedingMaxStep() {
        // Given
        ComboSystem combo = new ComboSystem(4, 2.0f);

        // When
        combo.tryAdvanceCombo(1000); // step 1
        combo.tryAdvanceCombo(1100); // step 2
        combo.tryAdvanceCombo(1200); // step 3
        combo.tryAdvanceCombo(1300); // step 4
        int step5 = combo.tryAdvanceCombo(1400); // should cycle to 1

        // Then
        assertThat(step5).isEqualTo(1);
    }

    @Test
    @DisplayName("should_returnCorrectAnimationTriggerName")
    void should_returnCorrectAnimationTriggerName() {
        // Given
        ComboSystem combo = new ComboSystem(4, 1.0f);
        combo.tryAdvanceCombo(1000); // step 1

        // When
        String trigger = combo.getAnimationTriggerName("Attack");

        // Then
        assertThat(trigger).isEqualTo("Attack_01");
    }
}
