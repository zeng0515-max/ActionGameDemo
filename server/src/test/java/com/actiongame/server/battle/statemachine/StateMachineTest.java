package com.actiongame.server.battle.statemachine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("状态机测试")
class StateMachineTest {

    @Test
    @DisplayName("should_enterState_when_setStateCalled")
    void should_enterState_when_setStateCalled() {
        // Given
        StateMachine sm = new StateMachine();
        TestState idle = new TestState("Idle");
        TestState attack = new TestState("Attack");
        sm.registerState("Idle", idle);
        sm.registerState("Attack", attack);

        // When
        sm.setState("Attack");

        // Then
        assertThat(sm.getCurrentStateName()).isEqualTo("Attack");
        assertThat(attack.enterCount).isEqualTo(1);
        assertThat(idle.exitCount).isEqualTo(0); // 从未进入Idle, 不触发onExit
    }

    @Test
    @DisplayName("should_callOnExit_when_switchingStates")
    void should_callOnExit_when_switchingStates() {
        // Given
        StateMachine sm = new StateMachine();
        TestState idle = new TestState("Idle");
        TestState attack = new TestState("Attack");
        sm.registerState("Idle", idle);
        sm.registerState("Attack", attack);
        sm.setState("Idle");

        // When
        sm.setState("Attack");

        // Then
        assertThat(idle.exitCount).isEqualTo(1);
        assertThat(attack.enterCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should_callOnUpdate_when_updateCalled")
    void should_callOnUpdate_when_updateCalled() {
        // Given
        StateMachine sm = new StateMachine();
        TestState state = new TestState("Idle");
        sm.registerState("Idle", state);
        sm.setState("Idle");

        // When
        sm.update(0.02f);
        sm.update(0.02f);

        // Then
        assertThat(state.updateCount).isEqualTo(2);
        assertThat(state.totalDelta).isEqualTo(0.04f, within(0.001f));
    }

    @Test
    @DisplayName("should_transition_when_conditionMet")
    void should_transition_when_conditionMet() {
        // Given
        StateMachine sm = new StateMachine();
        TestState idle = new TestState("Idle");
        TestState attack = new TestState("Attack");
        sm.registerState("Idle", idle);
        sm.registerState("Attack", attack);
        sm.setState("Idle");

        // Add transition: Idle → Attack when shouldAttack is true
        boolean[] flag = {false};
        sm.addTransition("Idle", "Attack", () -> flag[0]);

        // When
        flag[0] = true;
        sm.update(0.02f);

        // Then
        assertThat(sm.getCurrentStateName()).isEqualTo("Attack");
    }

    private static class TestState implements IState {
        final String name;
        int enterCount = 0;
        int exitCount = 0;
        int updateCount = 0;
        float totalDelta = 0f;

        TestState(String name) { this.name = name; }

        @Override public void onEnter() { enterCount++; }
        @Override public void onUpdate(float deltaTime) { updateCount++; totalDelta += deltaTime; }
        @Override public void onExit() { exitCount++; }
    }
}
