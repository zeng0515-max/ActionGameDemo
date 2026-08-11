package com.actiongame.server.domain.buff;

import com.actiongame.server.domain.combat.ElementType;

/**
 * Buff实体 (服务端权威)
 * 对应Unity BuffBase + BuffData, 纯数据, 效果逻辑在battle.buffengine层
 */
public class Buff {
    private final int buffId;
    private final String buffName;
    private final BuffType buffType;
    private final float duration;
    private float remainingTime;
    private int stacks = 1;
    private final int maxStacks;
    private final BuffStackingRule stackingRule;
    private final float tickInterval;
    private final float tickValuePercent;
    private final AttributeType attributeType;
    private final float attributeModifier;
    private final float shieldValue;
    private final ElementType relatedElement;
    private int targetEntityId;
    private int sourceEntityId;
    private boolean active = false;

    public Buff(int buffId, String buffName, BuffType buffType, float duration,
                int maxStacks, BuffStackingRule stackingRule,
                float tickInterval, float tickValuePercent,
                AttributeType attributeType, float attributeModifier,
                float shieldValue, ElementType relatedElement) {
        this.buffId = buffId;
        this.buffName = buffName;
        this.buffType = buffType;
        this.duration = duration;
        this.remainingTime = duration;
        this.maxStacks = maxStacks;
        this.stackingRule = stackingRule;
        this.tickInterval = tickInterval;
        this.tickValuePercent = tickValuePercent;
        this.attributeType = attributeType;
        this.attributeModifier = attributeModifier;
        this.shieldValue = shieldValue;
        this.relatedElement = relatedElement;
    }

    public void update(float deltaTime) {
        if (!active) return;
        remainingTime -= deltaTime;
        if (remainingTime < 0f) remainingTime = 0f;
    }

    public boolean isExpired() {
        return remainingTime <= 0f;
    }

    public void refreshDuration() {
        remainingTime = duration;
    }

    public void setRemainingTime(float remainingTime) {
        this.remainingTime = Math.max(0f, remainingTime);
    }

    public void setStacks(int stacks) {
        this.stacks = Math.min(stacks, maxStacks);
    }

    // === Getters ===

    public int getBuffId() { return buffId; }
    public String getBuffName() { return buffName; }
    public BuffType getBuffType() { return buffType; }
    public float getDuration() { return duration; }
    public float getRemainingTime() { return remainingTime; }
    public int getStacks() { return stacks; }
    public int getMaxStacks() { return maxStacks; }
    public BuffStackingRule getStackingRule() { return stackingRule; }
    public float getTickInterval() { return tickInterval; }
    public float getTickValuePercent() { return tickValuePercent; }
    public AttributeType getAttributeType() { return attributeType; }
    public float getAttributeModifier() { return attributeModifier; }
    public float getShieldValue() { return shieldValue; }
    public ElementType getRelatedElement() { return relatedElement; }
    public int getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(int targetEntityId) { this.targetEntityId = targetEntityId; }
    public int getSourceEntityId() { return sourceEntityId; }
    public void setSourceEntityId(int sourceEntityId) { this.sourceEntityId = sourceEntityId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
