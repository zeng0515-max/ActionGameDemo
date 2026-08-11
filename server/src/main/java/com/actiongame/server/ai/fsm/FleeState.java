package com.actiongame.server.ai.fsm;

import com.actiongame.server.ai.decision.EnemyAIController;

/**
 * 逃跑状态 (对应Unity FleeState)
 * 远离玩家, 直到脱离检测范围或恢复血量. 最大持续5秒
 */
public class FleeState extends EnemyState {
    private float fleeTimer;
    private float fleeUpdateTimer;
    private static final float FLEE_UPDATE_INTERVAL = 0.2f;
    private static final float FLEE_MAX_DURATION = 5f;

    public FleeState(EnemyAIController controller) {
        super(controller);
    }

    @Override
    public void onEnter() {
        fleeTimer = 0f;
        fleeUpdateTimer = 0f;
    }

    @Override
    public void onUpdate(float deltaTime) {
        fleeTimer += deltaTime;
        fleeUpdateTimer += deltaTime;

        if (fleeUpdateTimer >= FLEE_UPDATE_INTERVAL) {
            fleeUpdateTimer = 0f;
            controller.fleeFromTarget();
        }
    }

    @Override
    public void onExit() {
        controller.stopMoving();
    }

    public void checkSwitchState() {
        if (controller.isDead()) { switchTo(EnemyStateType.DEAD); return; }
        if (fleeTimer >= FLEE_MAX_DURATION || !controller.detectTarget()) { switchTo(EnemyStateType.IDLE); return; }
        if (!controller.shouldFlee()) { switchTo(EnemyStateType.IDLE); return; }
    }
}
