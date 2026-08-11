package com.actiongame.server.ai.fsm;

import com.actiongame.server.ai.decision.EnemyAIController;

/**
 * 待机状态 (对应Unity EnemyIdleState)
 * 停留原地, 检测到玩家→追击, 超时→巡逻
 */
public class IdleState extends EnemyState {
    private float idleTimer;

    public IdleState(EnemyAIController controller) {
        super(controller);
    }

    @Override
    public void onEnter() {
        idleTimer = 0f;
        controller.stopMoving();
    }

    @Override
    public void onUpdate(float deltaTime) {
        idleTimer += deltaTime;
    }

    @Override
    public void onExit() {
    }

    /**
     * 检查状态切换条件
     */
    public void checkSwitchState() {
        if (controller.isDead()) { switchTo(EnemyStateType.DEAD); return; }
        if (controller.detectTarget()) { switchTo(EnemyStateType.CHASE); return; }
        if (controller.shouldFlee() && controller.hasTarget()) { switchTo(EnemyStateType.FLEE); return; }
        if (idleTimer >= controller.getPatrolWaitTime()) { switchTo(EnemyStateType.PATROL); return; }
    }
}
