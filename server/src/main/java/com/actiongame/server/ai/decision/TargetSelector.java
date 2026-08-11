package com.actiongame.server.ai.decision;

import com.actiongame.server.domain.character.Character;
import com.actiongame.server.util.Vector3;

/**
 * 目标选择器 (对应文档3.4.3 TargetSelector)
 * 结合AggroSystem选择最优目标
 */
public class TargetSelector {
    private final com.actiongame.server.battle.combatsystem.AggroSystem aggroSystem;
    private final float detectionRange;
    private final float viewAngle;

    public TargetSelector(com.actiongame.server.battle.combatsystem.AggroSystem aggroSystem,
                          float detectionRange, float viewAngle) {
        this.aggroSystem = aggroSystem;
        this.detectionRange = detectionRange;
        this.viewAngle = viewAngle;
    }

    /**
     * 获取当前仇恨最高的目标
     */
    public int getTopAggroTarget() {
        return aggroSystem.getTopAggroTarget();
    }

    /**
     * 检查目标是否在检测范围内
     */
    public boolean isTargetInDetectionRange(Character self, Character target) {
        if (target == null || target.isDead()) return false;
        return self.getPosition().distance(target.getPosition()) <= detectionRange;
    }

    /**
     * 检查目标是否在追击范围 (检测范围 × 1.5)
     */
    public boolean isTargetInChaseRange(Character self, Character target) {
        if (target == null || target.isDead()) return false;
        return self.getPosition().distance(target.getPosition()) <= detectionRange * 1.5f;
    }

    /**
     * 检查目标是否在攻击范围内
     */
    public boolean isTargetInAttackRange(Character self, Character target, float attackRange) {
        if (target == null || target.isDead()) return false;
        return self.getPosition().distance(target.getPosition()) <= attackRange;
    }

    /**
     * 检查是否应逃跑 (血量低于逃跑阈值)
     */
    public boolean shouldFlee(Character self, float fleeThreshold) {
        return self.getStats().getHealthPercent() <= fleeThreshold;
    }
}
