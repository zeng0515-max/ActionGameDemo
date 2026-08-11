package com.actiongame.server.battle.statemachine;

/**
 * 状态接口 (通用状态机)
 */
public interface IState {
    void onEnter();

    void onUpdate(float deltaTime);

    void onExit();
}
