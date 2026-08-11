package com.actiongame.server.battle.buffengine;

import com.actiongame.server.config.BuffConfig;
import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.buff.BuffStackingRule;
import com.actiongame.server.domain.buff.BuffType;

/**
 * Buff工厂 (对应Unity BuffFactory)
 * 根据BuffConfig创建Buff实例并关联对应的IBuffEffectHandler
 */
public class BuffFactory {

    /**
     * 从配置创建Buff实体
     */
    public static Buff createBuff(BuffConfig config) {
        return new Buff(
            config.getBuffName().hashCode(),
            config.getBuffName(),
            config.getBuffType(),
            config.getDuration(),
            config.getMaxStacks(),
            config.getStackingRule(),
            config.getTickInterval(),
            config.getTickValuePercent(),
            config.getAttributeType(),
            config.getAttributeModifier(),
            config.getShieldValue(),
            config.getRelatedElement()
        );
    }

    /**
     * 根据Buff类型创建对应的IBuffEffectHandler
     */
    public static IBuffEffectHandler createEffectHandler(BuffType buffType) {
        return switch (buffType) {
            case ATTRIBUTE -> new AttributeBuffEffect();
            case DOT -> new DotBuffEffect();
            case CONTROL -> new ControlBuffEffect();
            case SHIELD -> new ShieldBuffEffect();
        };
    }

    /**
     * 根据buffName创建对应的IBuffEffectHandler (用于DoT/Attribute子类型区分)
     */
    public static IBuffEffectHandler createEffectHandler(BuffType buffType, String buffName) {
        return switch (buffType) {
            case ATTRIBUTE -> new AttributeBuffEffect();
            case DOT -> new DotBuffEffect();
            case CONTROL -> new ControlBuffEffect();
            case SHIELD -> new ShieldBuffEffect();
        };
    }
}
