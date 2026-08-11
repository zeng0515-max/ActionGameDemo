package com.actiongame.server.battle.pipeline;

import com.actiongame.server.domain.combat.DamageInfo;
import com.actiongame.server.util.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 伤害计算管线 (对应Unity DamagePipeline)
 * 5步流水线: 基础伤害 → 暴击 → 元素克制 → 防御减免 → 最终伤害
 * 每一步使用策略模式, 可插拔替换
 */
public class DamagePipeline {
    private final List<IDamageStep> steps;

    public DamagePipeline() {
        this.steps = new ArrayList<>();
        steps.add(new BaseDamageStep());
        steps.add(new CriticalDamageStep());
        steps.add(new ElementDamageStep());
        steps.add(new DefenseDamageStep());
    }

    public DamagePipeline(List<IDamageStep> customSteps) {
        this.steps = new ArrayList<>(customSteps);
    }

    /**
     * 执行完整伤害计算管线
     * 输入 DamageInfo 中的 skillMultiplier, attackElement, defenderElement 会被使用
     * 输出 DamageInfo 的 finalDamage 被更新为最终伤害值
     */
    public DamageInfo calculate(DamageInfo input, DamageContext context) {
        DamageInfo result = input;
        result.setAttackerEntityId(context.getAttackerEntityId());
        result.setTargetEntityId(context.getTargetEntityId());

        float currentDamage = 0f;
        for (IDamageStep step : steps) {
            currentDamage = step.execute(result, context);
            // 每步执行后, baseDamage 被更新为当前步骤的输出, 供下一步使用
            result.setBaseDamage(currentDamage);
        }

        return result;
    }

    /**
     * 简化版伤害计算 (不含随机暴击, criticalRate=0)
     */
    public DamageInfo calculateSimple(DamageInfo input, float attackPower, float defense,
                                      int attackerEntityId, int targetEntityId) {
        DamageContext context = new DamageContext(
            attackPower, defense, 0f, 1.5f, 1f, attackerEntityId, targetEntityId);
        return calculate(input, context);
    }

    public List<IDamageStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }
}
