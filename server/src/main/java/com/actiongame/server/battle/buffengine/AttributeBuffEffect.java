package com.actiongame.server.battle.buffengine;

import com.actiongame.server.domain.buff.AttributeType;
import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.domain.character.CharacterStats;

/**
 * 属性Buff效果 (对应Unity AttributeBuff)
 * 攻击力/防御力/移速/暴击率 增减
 * OnApply时增加属性, OnRemove时恢复
 */
public class AttributeBuffEffect implements IBuffEffectHandler {
    private int appliedStacks = 0;

    @Override
    public void onApply(Buff buff, Character target, Character source) {
        if (appliedStacks > 0) return;
        applyModifier(buff, target, buff.getStacks(), false);
        appliedStacks = buff.getStacks();
    }

    @Override
    public void onUpdate(Buff buff, Character target, Character source, float deltaTime) {
        // 属性Buff不需要每帧更新
    }

    @Override
    public void onRemove(Buff buff, Character target, Character source) {
        if (appliedStacks <= 0) return;
        applyModifier(buff, target, appliedStacks, true);
        appliedStacks = 0;
    }

    @Override
    public void onStack(Buff buff, Character target, Character source, int newStacks) {
        // 先移除旧层数效果
        if (appliedStacks > 0) {
            applyModifier(buff, target, appliedStacks, true);
        }
        // 应用新层数效果
        applyModifier(buff, target, newStacks, false);
        appliedStacks = newStacks;
    }

    private void applyModifier(Buff buff, Character target, int stacks, boolean reverse) {
        if (target == null) return;
        CharacterStats stats = target.getStats();
        float modifier = buff.getAttributeModifier() / 100f * stacks;
        if (reverse) modifier = -modifier;

        switch (buff.getAttributeType()) {
            case ATTACK_POWER -> stats.addAttackModifier(modifier);
            case DEFENSE -> stats.addDefenseModifier(modifier);
            case MOVE_SPEED -> stats.addMoveSpeedModifier(modifier);
            case CRITICAL_RATE -> stats.addCriticalRateModifier(modifier);
        }
    }
}
