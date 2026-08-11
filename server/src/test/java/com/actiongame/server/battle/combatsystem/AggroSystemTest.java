package com.actiongame.server.battle.combatsystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("仇恨系统测试")
class AggroSystemTest {

    @Test
    @DisplayName("should_addAggro_when_addAggroCalled")
    void should_addAggro_when_addAggroCalled() {
        // Given
        AggroSystem aggro = new AggroSystem(1);

        // When
        aggro.addAggro(101, 50f);
        aggro.addAggro(102, 30f);
        aggro.addAggro(101, 20f); // 累加

        // Then
        assertThat(aggro.getAggro(101)).isEqualTo(70f, within(0.001f));
        assertThat(aggro.getAggro(102)).isEqualTo(30f, within(0.001f));
    }

    @Test
    @DisplayName("should_returnTopTarget_when_getTopAggroTarget")
    void should_returnTopTarget_when_getTopAggroTarget() {
        // Given
        AggroSystem aggro = new AggroSystem(1);
        aggro.addAggro(101, 50f);
        aggro.addAggro(102, 100f);
        aggro.addAggro(103, 30f);

        // When
        int topTarget = aggro.getTopAggroTarget();

        // Then
        assertThat(topTarget).isEqualTo(102);
    }

    @Test
    @DisplayName("should_decayAllAggro_when_decayCalled")
    void should_decayAllAggro_when_decayCalled() {
        // Given
        AggroSystem aggro = new AggroSystem(1);
        aggro.addAggro(101, 100f);
        aggro.addAggro(102, 50f);

        // When: decay 50%
        aggro.decayAll(0.5f);

        // Then
        assertThat(aggro.getAggro(101)).isEqualTo(50f, within(0.001f));
        assertThat(aggro.getAggro(102)).isEqualTo(25f, within(0.001f));
    }

    @Test
    @DisplayName("should_removeLowAggro_when_decayBelowThreshold")
    void should_removeLowAggro_when_decayBelowThreshold() {
        // Given
        AggroSystem aggro = new AggroSystem(1);
        aggro.addAggro(101, 100f);
        aggro.addAggro(102, 1f); // Will decay to 0.5f < 1f threshold

        // When
        aggro.decayAll(0.5f);

        // Then
        assertThat(aggro.getAggro(101)).isEqualTo(50f, within(0.001f));
        assertThat(aggro.hasTarget()).isTrue();
        assertThat(aggro.getAggro(102)).isEqualTo(0f); // Removed
    }

    @Test
    @DisplayName("should_clearAllAggro_when_clearCalled")
    void should_clearAllAggro_when_clearCalled() {
        // Given
        AggroSystem aggro = new AggroSystem(1);
        aggro.addAggro(101, 50f);
        aggro.addAggro(102, 30f);

        // When
        aggro.clear();

        // Then
        assertThat(aggro.hasTarget()).isFalse();
        assertThat(aggro.getTopAggroTarget()).isEqualTo(-1);
    }

    @Test
    @DisplayName("should_removeTarget_when_removeTargetCalled")
    void should_removeTarget_when_removeTargetCalled() {
        // Given
        AggroSystem aggro = new AggroSystem(1);
        aggro.addAggro(101, 50f);
        aggro.addAggro(102, 100f);

        // When
        aggro.removeTarget(102);

        // Then: 102 removed, 101 becomes top
        assertThat(aggro.getTopAggroTarget()).isEqualTo(101);
    }
}
