package com.actiongame.server.battle.pipeline;

import com.actiongame.server.domain.combat.DamageInfo;

/**
 * 步骤1: 基础伤害 = 攻击力 × 技能倍率
 */
public class BaseDamageStep implements IDamageStep {
    @Override
    public float execute(DamageInfo info, DamageContext context) {
        float step1 = context.getAttackPower() * info.getSkillMultiplier();
        info.setBaseDamage(step1);
        return step1;
    }
}
