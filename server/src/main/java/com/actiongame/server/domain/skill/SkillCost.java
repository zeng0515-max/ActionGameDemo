package com.actiongame.server.domain.skill;

/**
 * 技能消耗
 */
public class SkillCost {
    private float energyCost;
    private float healthCost;

    public SkillCost() {
        this.energyCost = 0f;
        this.healthCost = 0f;
    }

    public SkillCost(float energyCost, float healthCost) {
        this.energyCost = energyCost;
        this.healthCost = healthCost;
    }

    public float getEnergyCost() { return energyCost; }
    public void setEnergyCost(float energyCost) { this.energyCost = energyCost; }

    public float getHealthCost() { return healthCost; }
    public void setHealthCost(float healthCost) { this.healthCost = healthCost; }

    public boolean hasCost() {
        return energyCost > 0f || healthCost > 0f;
    }
}
