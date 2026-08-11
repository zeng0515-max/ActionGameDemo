package com.actiongame.server.ai.fsm;

import com.actiongame.server.ai.decision.EnemyAIController;
import com.actiongame.server.util.Vector3;

/**
 * 巡逻状态 (对应Unity PatrolState)
 * 在出生点周围随机移动, 到达后等待. 检测到玩家→追击
 */
public class PatrolState extends EnemyState {
    private Vector3 patrolDestination;
    private boolean isMoving;
    private float waitTimer;

    public PatrolState(EnemyAIController controller) {
        super(controller);
    }

    @Override
    public void onEnter() {
        waitTimer = 0f;
        isMoving = false;
        pickNewDestination();
    }

    @Override
    public void onUpdate(float deltaTime) {
        if (isMoving) {
            if (controller.hasReached(patrolDestination)) {
                isMoving = false;
                controller.stopMoving();
                waitTimer = 0f;
            }
        } else {
            waitTimer += deltaTime;
        }
    }

    @Override
    public void onExit() {
    }

    public void checkSwitchState() {
        if (controller.isDead()) { switchTo(EnemyStateType.DEAD); return; }
        if (controller.detectTarget()) { switchTo(EnemyStateType.CHASE); return; }
        if (controller.shouldFlee() && controller.hasTarget()) { switchTo(EnemyStateType.FLEE); return; }
        if (!isMoving && waitTimer >= controller.getPatrolWaitTime()) { pickNewDestination(); }
    }

    private void pickNewDestination() {
        patrolDestination = controller.getRandomPatrolPoint();
        controller.moveTo(patrolDestination);
        isMoving = true;
    }
}
