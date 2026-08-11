package com.actiongame.server.battle.buffengine;

import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.domain.character.CharacterStats;

/**
 * Buff效果接口 (服务端战斗系统调用)
 * 直接操作Character, 不依赖GameObject
 */
public interface IBuffEffectHandler {
    void onApply(Buff buff, Character target, Character source);

    void onUpdate(Buff buff, Character target, Character source, float deltaTime);

    void onRemove(Buff buff, Character target, Character source);

    void onStack(Buff buff, Character target, Character source, int newStacks);
}
