package com.actiongame.server.battle.statemachine;

/**
 * 状态转移定义
 */
public class StateTransition {
    private final String fromState;
    private final String toState;
    private final TransitionCondition condition;

    public interface TransitionCondition {
        boolean shouldTransition();
    }

    public StateTransition(String fromState, String toState, TransitionCondition condition) {
        this.fromState = fromState;
        this.toState = toState;
        this.condition = condition;
    }

    public String getFromState() { return fromState; }
    public String getToState() { return toState; }
    public TransitionCondition getCondition() { return condition; }
}
