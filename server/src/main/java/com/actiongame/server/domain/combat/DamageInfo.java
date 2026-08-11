package com.actiongame.server.domain.combat;

import com.actiongame.server.util.Vector3;

/**
 * 伤害信息 (对应Unity DamageInfo struct)
 * 传递完整伤害计算数据, 贯穿DamagePipeline 5步管线
 */
public class DamageInfo {
    private float baseDamage;
    private float skillMultiplier;
    private boolean isCritical;
    private float criticalDamage;
    private float elementMultiplier;
    private float defense;
    private float finalDamage;
    private Vector3 hitPosition;
    private int attackerEntityId;
    private int targetEntityId;
    private ElementType attackElement;
    private ElementType defenderElement;

    public DamageInfo() {
        this.skillMultiplier = 1f;
        this.elementMultiplier = 1f;
        this.criticalDamage = 1f;
        this.attackElement = ElementType.NONE;
        this.defenderElement = ElementType.NONE;
        this.hitPosition = Vector3.ZERO;
    }

    public float getBaseDamage() { return baseDamage; }
    public void setBaseDamage(float baseDamage) { this.baseDamage = baseDamage; }

    public float getSkillMultiplier() { return skillMultiplier; }
    public void setSkillMultiplier(float skillMultiplier) { this.skillMultiplier = skillMultiplier; }

    public boolean isCritical() { return isCritical; }
    public void setCritical(boolean critical) { isCritical = critical; }

    public float getCriticalDamage() { return criticalDamage; }
    public void setCriticalDamage(float criticalDamage) { this.criticalDamage = criticalDamage; }

    public float getElementMultiplier() { return elementMultiplier; }
    public void setElementMultiplier(float elementMultiplier) { this.elementMultiplier = elementMultiplier; }

    public float getDefense() { return defense; }
    public void setDefense(float defense) { this.defense = defense; }

    public float getFinalDamage() { return finalDamage; }
    public void setFinalDamage(float finalDamage) { this.finalDamage = finalDamage; }

    public Vector3 getHitPosition() { return hitPosition; }
    public void setHitPosition(Vector3 hitPosition) { this.hitPosition = hitPosition; }

    public int getAttackerEntityId() { return attackerEntityId; }
    public void setAttackerEntityId(int attackerEntityId) { this.attackerEntityId = attackerEntityId; }

    public int getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(int targetEntityId) { this.targetEntityId = targetEntityId; }

    public ElementType getAttackElement() { return attackElement; }
    public void setAttackElement(ElementType attackElement) { this.attackElement = attackElement; }

    public ElementType getDefenderElement() { return defenderElement; }
    public void setDefenderElement(ElementType defenderElement) { this.defenderElement = defenderElement; }

    public boolean isValid() {
        return attackerEntityId > 0 || finalDamage > 0f;
    }

    public boolean isElemental() {
        return attackElement != ElementType.NONE;
    }

    public boolean isCountering() {
        return isCountering(attackElement, defenderElement);
    }

    public static boolean isCountering(ElementType attack, ElementType defender) {
        if (attack == ElementType.NONE || defender == ElementType.NONE) return false;
        return (attack == ElementType.FIRE && defender == ElementType.ICE)
            || (attack == ElementType.ICE && defender == ElementType.LIGHTNING)
            || (attack == ElementType.LIGHTNING && defender == ElementType.WATER)
            || (attack == ElementType.WATER && defender == ElementType.FIRE);
    }

    /**
     * 查询攻击元素对防御元素的倍率
     */
    public static float getMultiplier(ElementType attack, ElementType defender) {
        if (attack == ElementType.NONE || defender == ElementType.NONE) return 1f;
        if (isCountering(attack, defender)) return 1.5f;
        return 1f;
    }

    @Override
    public String toString() {
        return String.format("DamageInfo{base=%.1f, skill=%.2f, crit=%s, elem=%.2f, def=%.1f, final=%.1f, %s→%s}",
            baseDamage, skillMultiplier, isCritical, elementMultiplier, defense, finalDamage,
            attackElement, defenderElement);
    }
}
