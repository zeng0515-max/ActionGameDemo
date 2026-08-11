package com.actiongame.server.battle.buffengine;

import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.character.Character;

/**
 * 护盾Buff效果 (对应Unity ShieldBuff)
 * 吸收伤害, 可叠加层数
 */
public class ShieldBuffEffect implements IBuffEffectHandler {
    private float shieldAmount = 0f;

    @Override
    public void onApply(Buff buff, Character target, Character source) {
        if (target == null) return;
        shieldAmount = buff.getShieldValue() * buff.getStacks();
        target.addShield(shieldAmount);
    }

    @Override
    public void onUpdate(Buff buff, Character target, Character source, float deltaTime) {
        // 护盾不需要每帧更新
    }

    @Override
    public void onRemove(Buff buff, Character target, Character source) {
        if (target == null) return;
        if (shieldAmount > 0f) {
            target.removeShield(shieldAmount);
        }
    }

    @Override
    public void onStack(Buff buff, Character target, Character source, int newStacks) {
        if (target == null) return;
        // 先移除旧护盾
        if (shieldAmount > 0f) {
            target.removeShield(shieldAmount);
        }
        // 添加新护盾
        shieldAmount = buff.getShieldValue() * newStacks;
        target.addShield(shieldAmount);
    }

    public float getShieldAmount() { return shieldAmount; }
    public boolean isShieldBroken() { return shieldAmount <= 0f; }
}
