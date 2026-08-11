package com.actiongame.server.ai.fsm;

import com.actiongame.server.ai.decision.EnemyAIController;

/**
 * 追击状态 (对应Unity ChaseState)
 * 定期更新路径追踪玩家. 目标丢失/脱离→待机
 * 注意: 敌人不攻击玩家, 仅追踪
 */
public class ChaseState extends EnemyState {
    private float pathUpdateTimer;
    private static final float PATH_UPDATE_INTERVAL = 0.3f;

    public ChaseState(EnemyAIController controller) {
        super(controller);
    }

    @Override
    public void onEnter() {
        pathUpdateTimer = 0f;
    }

    @Override
    public void onUpdate(float deltaTime) {
        pathUpdateTimer += deltaTime;
        if (pathUpdateTimer >= PATH_UPDATE_INTERVAL) {
            pathUpdateTimer = 0f;
            if (controller.getTarget() != null) {
                controller.moveTo(controller.getTargetPosition());
            }
        }
    }

    @Override
    public void onExit() {
    }

    public void checkSwitchState() {
        if (controller.isDead()) { switchTo(EnemyStateType.DEAD); return; }
        if (!controller.hasTarget() || controller.isTargetDead()) {
            controller.clearTarget();
            switchTo(EnemyStateType.IDLE);
            return;
        }
        if (controller.shouldFlee()) { switchTo(EnemyStateType.FLEE); return; }
        if (!controller.isTargetInChaseRange()) { switchTo(EnemyStateType.IDLE); return; }
    }
}
