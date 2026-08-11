package com.actiongame.server.ai.fsm;

import com.actiongame.server.ai.decision.EnemyAIController;

/**
 * 死亡状态 (对应Unity EnemyDeadState)
 * 停止移动, 终态不切换
 */
public class DeadState extends EnemyState {
    public DeadState(EnemyAIController controller) {
        super(controller);
    }

    @Override
    public void onEnter() {
        controller.stopMoving();
    }

    @Override
    public void onUpdate(float deltaTime) {
        // 死亡状态不执行逻辑更新
    }

    @Override
    public void onExit() {
    }

    public void checkSwitchState() {
        // 死亡是终态, 不切换
    }
}
