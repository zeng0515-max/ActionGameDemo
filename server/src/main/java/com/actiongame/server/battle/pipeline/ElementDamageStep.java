package com.actiongame.server.battle.pipeline;

import com.actiongame.server.domain.combat.DamageInfo;

/**
 * 步骤3: 元素克制 = × 元素倍率
 * 火克冰/冰克雷/雷克水/水克火, 克制倍率1.5x
 */
public class ElementDamageStep implements IDamageStep {
    @Override
    public float execute(DamageInfo info, DamageContext context) {
        float elemMult = DamageInfo.getMultiplier(info.getAttackElement(), info.getDefenderElement());
        info.setElementMultiplier(elemMult);
        return info.getBaseDamage() * elemMult;
    }
}
