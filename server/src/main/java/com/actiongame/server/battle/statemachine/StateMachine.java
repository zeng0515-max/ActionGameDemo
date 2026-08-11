package com.actiongame.server.battle.statemachine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用状态机 (对应Unity StateMachine)
 * 管理状态注册、切换、转移条件
 */
public class StateMachine {
    private final Map<String, IState> states = new HashMap<>();
    private final List<StateTransition> transitions = new ArrayList<>();
    private IState currentState;
    private String currentStateName;

    public void registerState(String name, IState state) {
        states.put(name, state);
    }

    public void addTransition(String from, String to, StateTransition.TransitionCondition condition) {
        transitions.add(new StateTransition(from, to, condition));
    }

    public void setState(String name) {
        if (!states.containsKey(name)) return;

        if (currentState != null) {
            currentState.onExit();
        }

        currentStateName = name;
        currentState = states.get(name);
        currentState.onEnter();
    }

    public void update(float deltaTime) {
        if (currentState == null) return;

        currentState.onUpdate(deltaTime);

        // 检查转移条件
        for (StateTransition transition : transitions) {
            if (transition.getFromState().equals(currentStateName)) {
                if (transition.getCondition().shouldTransition()) {
                    setState(transition.getToState());
                    return;
                }
            }
        }
    }

    public IState getCurrentState() { return currentState; }
    public String getCurrentStateName() { return currentStateName; }
    public boolean isInState(String name) { return name.equals(currentStateName); }
}
