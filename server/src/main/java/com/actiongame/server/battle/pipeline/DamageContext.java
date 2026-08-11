package com.actiongame.server.battle.pipeline;

/**
 * 伤害计算上下文 (携带攻击者/防御者属性)
 */
public class DamageContext {
    private final float attackPower;
    private final float defense;
    private final float criticalRate;
    private final float criticalDamageMultiplier;
    private final float damageTakenMultiplier;
    private final int attackerEntityId;
    private final int targetEntityId;

    public DamageContext(float attackPower, float defense,
                         float criticalRate, float criticalDamageMultiplier,
                         float damageTakenMultiplier,
                         int attackerEntityId, int targetEntityId) {
        this.attackPower = attackPower;
        this.defense = defense;
        this.criticalRate = criticalRate;
        this.criticalDamageMultiplier = criticalDamageMultiplier;
        this.damageTakenMultiplier = damageTakenMultiplier;
        this.attackerEntityId = attackerEntityId;
        this.targetEntityId = targetEntityId;
    }

    public float getAttackPower() { return attackPower; }
    public float getDefense() { return defense; }
    public float getCriticalRate() { return criticalRate; }
    public float getCriticalDamageMultiplier() { return criticalDamageMultiplier; }
    public float getDamageTakenMultiplier() { return damageTakenMultiplier; }
    public int getAttackerEntityId() { return attackerEntityId; }
    public int getTargetEntityId() { return targetEntityId; }
}
