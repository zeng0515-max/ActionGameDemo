package com.actiongame.server.battle.buffengine;

import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.domain.character.CharacterStats;

/**
 * 持续伤害Buff效果 (对应Unity DotBuff)
 * 按tickInterval间隔造成伤害, 伤害值 = 攻击力 × tickValuePercent%
 */
public class DotBuffEffect implements IBuffEffectHandler {
    private float tickTimer = 0f;

    @Override
    public void onApply(Buff buff, Character target, Character source) {
        tickTimer = 0f;
    }

    @Override
    public void onUpdate(Buff buff, Character target, Character source, float deltaTime) {
        tickTimer += deltaTime;
        if (tickTimer >= buff.getTickInterval()) {
            tickTimer = 0f;
            applyTickDamage(buff, target, source);
        }
    }

    @Override
    public void onRemove(Buff buff, Character target, Character source) {
        // DoT移除时无特殊处理
    }

    @Override
    public void onStack(Buff buff, Character target, Character source, int newStacks) {
        // DoT不叠加层数效果, 由堆叠规则处理刷新时间
    }

    private void applyTickDamage(Buff buff, Character target, Character source) {
        if (target == null || target.isDead()) return;

        float attackPower = source != null ? source.getStats().getEffectiveAttackPower() : 10f;
        float damage = attackPower * (buff.getTickValuePercent() / 100f);

        target.takeDamage(damage);
    }
}
