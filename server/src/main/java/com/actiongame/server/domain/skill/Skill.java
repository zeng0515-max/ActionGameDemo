package com.actiongame.server.domain.skill;

/**
 * 技能实体 (服务端权威)
 * 对应Unity SkillData, 纯数据
 */
public class Skill {
    private final int skillId;
    private final String skillName;
    private final SkillType skillType;
    private final float damageMultiplier;
    private final float cooldown;
    private final float range;
    private final int comboIndex;
    private final boolean canCancel;
    private final String animationTriggerName;
    private final SkillCost cost;
    private float currentCooldown;

    public Skill(int skillId, String skillName, SkillType skillType,
                 float damageMultiplier, float cooldown, float range,
                 int comboIndex, boolean canCancel, String animationTriggerName,
                 SkillCost cost) {
        this.skillId = skillId;
        this.skillName = skillName;
        this.skillType = skillType;
        this.damageMultiplier = damageMultiplier;
        this.cooldown = cooldown;
        this.range = range;
        this.comboIndex = comboIndex;
        this.canCancel = canCancel;
        this.animationTriggerName = animationTriggerName;
        this.cost = cost;
        this.currentCooldown = 0f;
    }

    public boolean isReady() {
        return currentCooldown <= 0f;
    }

    public void startCooldown() {
        currentCooldown = cooldown;
    }

    public void updateCooldown(float deltaTime) {
        if (currentCooldown > 0f) {
            currentCooldown -= deltaTime;
            if (currentCooldown < 0f) currentCooldown = 0f;
        }
    }

    public int getSkillId() { return skillId; }
    public String getSkillName() { return skillName; }
    public SkillType getSkillType() { return skillType; }
    public float getDamageMultiplier() { return damageMultiplier; }
    public float getCooldown() { return cooldown; }
    public float getRange() { return range; }
    public int getComboIndex() { return comboIndex; }
    public boolean isCanCancel() { return canCancel; }
    public String getAnimationTriggerName() { return animationTriggerName; }
    public SkillCost getCost() { return cost; }
    public float getCurrentCooldown() { return currentCooldown; }
}
