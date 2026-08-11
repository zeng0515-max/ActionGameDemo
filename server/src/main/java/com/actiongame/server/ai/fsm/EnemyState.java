package com.actiongame.server.ai.fsm;

import com.actiongame.server.ai.decision.EnemyAIController;
import com.actiongame.server.battle.statemachine.IState;

/**
 * 敌人状态基类 (对应Unity EnemyState)
 * 提供对EnemyAIController的快捷访问和状态切换
 */
public abstract class EnemyState implements IState {
    protected final EnemyAIController controller;

    protected EnemyState(EnemyAIController controller) {
        this.controller = controller;
    }

    protected void switchTo(EnemyStateType type) {
        controller.changeState(type);
    }
}
