package com.actiongame.server.config;

import com.actiongame.server.domain.buff.AttributeType;
import com.actiongame.server.domain.buff.BuffStackingRule;
import com.actiongame.server.domain.buff.BuffType;
import com.actiongame.server.domain.combat.ElementType;

/**
 * Buff配置 (对应Unity BuffData ScriptableObject)
 * 从JSON加载, 服务端权威
 */
public class BuffConfig {
    private String buffName = "New Buff";
    private BuffType buffType = BuffType.ATTRIBUTE;
    private String description = "";
    private float duration = 5f;
    private BuffStackingRule stackingRule = BuffStackingRule.REFRESH_DURATION;
    private int maxStacks = 1;
    private float tickInterval = 1f;
    private float tickValuePercent = 5f;
    private AttributeType attributeType = AttributeType.ATTACK_POWER;
    private float attributeModifier = 20f;
    private float shieldValue = 100f;
    private ElementType relatedElement = ElementType.NONE;

    public String getBuffName() { return buffName; }
    public void setBuffName(String buffName) { this.buffName = buffName; }
    public BuffType getBuffType() { return buffType; }
    public void setBuffType(BuffType buffType) { this.buffType = buffType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public float getDuration() { return duration; }
    public void setDuration(float duration) { this.duration = duration; }
    public BuffStackingRule getStackingRule() { return stackingRule; }
    public void setStackingRule(BuffStackingRule stackingRule) { this.stackingRule = stackingRule; }
    public int getMaxStacks() { return maxStacks; }
    public void setMaxStacks(int maxStacks) { this.maxStacks = maxStacks; }
    public float getTickInterval() { return tickInterval; }
    public void setTickInterval(float tickInterval) { this.tickInterval = tickInterval; }
    public float getTickValuePercent() { return tickValuePercent; }
    public void setTickValuePercent(float tickValuePercent) { this.tickValuePercent = tickValuePercent; }
    public AttributeType getAttributeType() { return attributeType; }
    public void setAttributeType(AttributeType attributeType) { this.attributeType = attributeType; }
    public float getAttributeModifier() { return attributeModifier; }
    public void setAttributeModifier(float attributeModifier) { this.attributeModifier = attributeModifier; }
    public float getShieldValue() { return shieldValue; }
    public void setShieldValue(float shieldValue) { this.shieldValue = shieldValue; }
    public ElementType getRelatedElement() { return relatedElement; }
    public void setRelatedElement(ElementType relatedElement) { this.relatedElement = relatedElement; }

    @Override
    public String toString() {
        return "BuffConfig{name=" + buffName + ", type=" + buffType + ", dur=" + duration + "}";
    }
}
