package com.actiongame.server.config;

/**
 * 技能配置 (对应Unity SkillData ScriptableObject)
 * 从JSON加载, 服务端权威
 */
public class SkillConfig {
    private String skillName;
    private float damageMultiplier;
    private float cooldown;
    private float range;
    private int comboIndex;
    private boolean canCancel;
    private String animationTriggerName = "Attack";

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public float getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(float damageMultiplier) { this.damageMultiplier = damageMultiplier; }
    public float getCooldown() { return cooldown; }
    public void setCooldown(float cooldown) { this.cooldown = cooldown; }
    public float getRange() { return range; }
    public void setRange(float range) { this.range = range; }
    public int getComboIndex() { return comboIndex; }
    public void setComboIndex(int comboIndex) { this.comboIndex = comboIndex; }
    public boolean isCanCancel() { return canCancel; }
    public void setCanCancel(boolean canCancel) { this.canCancel = canCancel; }
    public String getAnimationTriggerName() { return animationTriggerName; }
    public void setAnimationTriggerName(String animationTriggerName) { this.animationTriggerName = animationTriggerName; }

    @Override
    public String toString() {
        return "SkillConfig{name=" + skillName + ", dmgMult=" + damageMultiplier + ", cd=" + cooldown + "}";
    }
}
