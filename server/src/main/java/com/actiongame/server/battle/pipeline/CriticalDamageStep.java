package com.actiongame.server.battle.pipeline;

import com.actiongame.server.domain.combat.DamageInfo;
import com.actiongame.server.util.RandomUtil;

/**
 * 步骤2: 暴击计算
 * 如果输入已标记 isCritical 则直接使用; 否则按暴击率用RandomUtil随机判定
 * 暴击时伤害 × 暴击倍率
 */
public class CriticalDamageStep implements IDamageStep {
    @Override
    public float execute(DamageInfo info, DamageContext context) {
        boolean isCrit = info.isCritical();
        if (!isCrit && context.getCriticalRate() > 0f) {
            isCrit = RandomUtil.chance(context.getCriticalRate());
        }

        float critMult = isCrit ? context.getCriticalDamageMultiplier() : 1f;
        float step2 = info.getBaseDamage() * critMult;

        info.setCritical(isCrit);
        info.setCriticalDamage(critMult);
        return step2;
    }
}
