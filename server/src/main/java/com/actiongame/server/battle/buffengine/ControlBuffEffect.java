package com.actiongame.server.battle.buffengine;

import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.domain.character.CharacterStats;

/**
 * 控制Buff效果 (对应Unity FreezeBuff + ShockBuff)
 * 冰冻: 移速降低50%
 * 感电: 受伤增加25%
 * 通过buffName区分具体效果
 */
public class ControlBuffEffect implements IBuffEffectHandler {
    private float speedReduction = 0.5f;
    private float damageTakenIncrease = 0.25f;

    @Override
    public void onApply(Buff buff, Character target, Character source) {
        if (target == null) return;
        CharacterStats stats = target.getStats();

        String name = buff.getBuffName() != null ? buff.getBuffName().toLowerCase() : "";
        if (name.contains("freeze") || name.contains("冰冻")) {
            stats.addMoveSpeedModifier(-speedReduction);
        } else if (name.contains("shock") || name.contains("感电")) {
            stats.setDamageTakenMultiplier(stats.getDamageTakenMultiplier() + damageTakenIncrease);
        }
    }

    @Override
    public void onUpdate(Buff buff, Character target, Character source, float deltaTime) {
        // 控制Buff不需要每帧更新数值
    }

    @Override
    public void onRemove(Buff buff, Character target, Character source) {
        if (target == null) return;
        CharacterStats stats = target.getStats();

        String name = buff.getBuffName() != null ? buff.getBuffName().toLowerCase() : "";
        if (name.contains("freeze") || name.contains("冰冻")) {
            stats.addMoveSpeedModifier(speedReduction);
        } else if (name.contains("shock") || name.contains("感电")) {
            stats.setDamageTakenMultiplier(Math.max(0f, stats.getDamageTakenMultiplier() - damageTakenIncrease));
        }
    }

    @Override
    public void onStack(Buff buff, Character target, Character source, int newStacks) {
        // 控制Buff不叠加层数, 刷新时间
    }
}
