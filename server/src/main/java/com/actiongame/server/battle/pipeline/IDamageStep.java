package com.actiongame.server.battle.pipeline;

import com.actiongame.server.domain.combat.DamageInfo;

/**
 * 伤害步骤接口 (策略模式, 每一步可插拔)
 */
public interface IDamageStep {
    /**
     * 执行伤害计算步骤
     * @param info 当前伤害信息 (可被修改)
     * @param context 伤害计算上下文
     * @return 处理后的伤害值
     */
    float execute(DamageInfo info, DamageContext context);
}
