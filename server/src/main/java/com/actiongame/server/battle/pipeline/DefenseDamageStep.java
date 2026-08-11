package com.actiongame.server.battle.pipeline;

import com.actiongame.server.domain.combat.DamageInfo;
import com.actiongame.server.util.MathUtils;

/**
 * 步骤4+5: 防御减免 + 受伤加成 + 最终伤害
 */
public class DefenseDamageStep implements IDamageStep {
    @Override
    public float execute(DamageInfo info, DamageContext context) {
        // 步骤4: 防御减免 (不低于0)
        float step4 = info.getBaseDamage() - context.getDefense();
        step4 = MathUtils.max(0f, step4);
        info.setDefense(context.getDefense());

        // 步骤4.5: 受伤加成倍率
        float step4b = step4 * context.getDamageTakenMultiplier();

        // 步骤5: 最终伤害 = max(1, 结果)
        float finalDamage = MathUtils.max(1f, step4b);
        info.setFinalDamage(finalDamage);
        return finalDamage;
    }
}
