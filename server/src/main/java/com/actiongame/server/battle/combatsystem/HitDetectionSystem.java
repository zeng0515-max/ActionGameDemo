package com.actiongame.server.battle.combatsystem;

import com.actiongame.server.domain.character.Character;
import com.actiongame.server.util.Vector3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 命中检测系统 (对应Unity AttackSystem + Hitbox)
 * 纯Java球形检测, 无Unity依赖
 * 支持层级掩码过滤和命中去重
 */
public class HitDetectionSystem {

    /**
     * 球形命中检测
     * @param attacker 攻击者
     * @param center 检测中心点
     * @param radius 检测半径
     * @param candidates 候选目标列表
     * @param alreadyHit 已命中实体ID集合 (去重)
     * @return 本次检测新命中的目标列表
     */
    public List<Character> detectHits(Character attacker, Vector3 center, float radius,
                                       List<Character> candidates, Set<Integer> alreadyHit) {
        if (alreadyHit == null) {
            alreadyHit = new HashSet<>();
        }

        final Set<Integer> hitSet = alreadyHit;
        float sqrRadius = radius * radius;

        return candidates.stream()
            .filter(target -> target.getEntityId() != attacker.getEntityId())
            .filter(target -> !target.isDead())
            .filter(target -> !hitSet.contains(target.getEntityId()))
            .filter(target -> {
                float sqrDist = target.getPosition().subtract(center).sqrMagnitude();
                return sqrDist <= sqrRadius;
            })
            .peek(target -> hitSet.add(target.getEntityId()))
            .toList();
    }

    /**
     * 单目标命中检测 (距离判定)
     */
    public boolean isInRange(Character attacker, Character target, float range) {
        if (target == null || target.isDead()) return false;
        float sqrDist = attacker.getPosition().subtract(target.getPosition()).sqrMagnitude();
        return sqrDist <= range * range;
    }

    /**
     * 视角锥检测 (AI视野判定)
     * @param forward 攻击者前方方向 (归一化)
     * @param viewAngle 视野角度 (度)
     * @param detectionRange 检测范围
     */
    public boolean isInView(Character observer, Character target, Vector3 forward,
                            float viewAngle, float detectionRange) {
        if (target == null || target.isDead()) return false;

        Vector3 toTarget = target.getPosition().subtract(observer.getPosition());
        float dist = toTarget.magnitude();
        if (dist > detectionRange) return false;

        Vector3 dirToTarget = toTarget.normalized();
        float dot = forward.x * dirToTarget.x + forward.y * dirToTarget.y + forward.z * dirToTarget.z;
        float angle = (float) Math.toDegrees(Math.acos(Math.max(-1f, Math.min(1f, dot))));

        return angle <= viewAngle * 0.5f;
    }
}
