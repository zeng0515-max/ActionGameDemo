package com.actiongame.server.ai.fsm;

import com.actiongame.server.ai.decision.EnemyAIController;

/**
 * 受击状态 (对应Unity EnemyHurtState)
 * 短暂硬直后恢复之前的行为
 */
public class HurtState extends EnemyState {
    private float hurtTimer;

    public HurtState(EnemyAIController controller) {
        super(controller);
    }

    @Override
    public void onEnter() {
        hurtTimer = 0f;
        controller.stopMoving();
    }

    @Override
    public void onUpdate(float deltaTime) {
        hurtTimer += deltaTime;
    }

    @Override
    public void onExit() {
    }

    public void checkSwitchState() {
        if (controller.isDead()) { switchTo(EnemyStateType.DEAD); return; }

        if (hurtTimer >= controller.getHurtRecoveryTime()) {
            if (controller.shouldFlee() && controller.hasTarget()) { switchTo(EnemyStateType.FLEE); return; }
            if (controller.hasTarget() && controller.detectTarget()) { switchTo(EnemyStateType.CHASE); return; }
            switchTo(EnemyStateType.IDLE);
        }
    }
}
