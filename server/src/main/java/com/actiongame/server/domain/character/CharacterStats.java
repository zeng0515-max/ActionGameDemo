package com.actiongame.server.domain.character;

import com.actiongame.server.util.MathUtils;

/**
 * 角色属性 (服务端权威, 禁止下发到客户端缓存)
 * 对应Unity CharacterStats, 但去除MonoBehaviour依赖
 */
public class CharacterStats {
    // 基础属性
    private float maxHealth;
    private float currentHealth;
    private float attackPower;
    private float defense;
    private float moveSpeed;
    private float criticalRate;
    private float criticalDamageMultiplier;
    private float maxEnergy;
    private float currentEnergy;

    // Buff修饰符 (百分比, 0 = 无加成)
    private float attackModifier = 0f;
    private float defenseModifier = 0f;
    private float moveSpeedModifier = 0f;
    private float criticalRateModifier = 0f;
    private float damageTakenMultiplier = 1f;

    public CharacterStats() {
        this.maxHealth = 100f;
        this.currentHealth = 100f;
        this.attackPower = 10f;
        this.defense = 5f;
        this.moveSpeed = 5f;
        this.criticalRate = 0.1f;
        this.criticalDamageMultiplier = 1.5f;
        this.maxEnergy = 100f;
        this.currentEnergy = 100f;
    }

    public CharacterStats(float maxHealth, float attackPower, float defense, float moveSpeed,
                          float criticalRate, float criticalDamageMultiplier) {
        this.maxHealth = MathUtils.max(1f, maxHealth);
        this.currentHealth = this.maxHealth;
        this.attackPower = MathUtils.max(0f, attackPower);
        this.defense = MathUtils.max(0f, defense);
        this.moveSpeed = MathUtils.max(0f, moveSpeed);
        this.criticalRate = MathUtils.clamp01(criticalRate);
        this.criticalDamageMultiplier = criticalDamageMultiplier;
        this.maxEnergy = 100f;
        this.currentEnergy = 100f;
    }

    // === 有效属性 (基础 + Buff修饰符) ===

    public float getEffectiveAttackPower() {
        return attackPower * (1f + attackModifier);
    }

    public float getEffectiveDefense() {
        return defense * (1f + defenseModifier);
    }

    public float getEffectiveMoveSpeed() {
        return moveSpeed * (1f + moveSpeedModifier);
    }

    public float getEffectiveCriticalRate() {
        return MathUtils.clamp01(criticalRate + criticalRateModifier);
    }

    // === 生命值操作 ===

    public void modifyHealth(float delta) {
        currentHealth = MathUtils.clamp(currentHealth + delta, 0f, maxHealth);
    }

    public void resetToFullHealth() {
        currentHealth = maxHealth;
    }

    public float getHealthPercent() {
        return maxHealth > 0f ? currentHealth / maxHealth : 0f;
    }

    public boolean isFullHealth() {
        return MathUtils.approximately(currentHealth, maxHealth);
    }

    public boolean isDead() {
        return currentHealth <= 0f;
    }

    // === 能量操作 ===

    public void modifyEnergy(float delta) {
        currentEnergy = MathUtils.clamp(currentEnergy + delta, 0f, maxEnergy);
    }

    // === 修饰符管理 ===

    public void addAttackModifier(float percentage) {
        attackModifier += percentage;
    }

    public void addDefenseModifier(float percentage) {
        defenseModifier += percentage;
    }

    public void addMoveSpeedModifier(float percentage) {
        moveSpeedModifier += percentage;
    }

    public void addCriticalRateModifier(float percentage) {
        criticalRateModifier += percentage;
    }

    public void resetAllModifiers() {
        attackModifier = 0f;
        defenseModifier = 0f;
        moveSpeedModifier = 0f;
        criticalRateModifier = 0f;
        damageTakenMultiplier = 1f;
    }

    public void recalculateStats(float baseHealth, float baseAttack, float baseDefense, float baseSpeed,
                                  float healthMult, float attackMult, float defenseMult, float speedMult) {
        this.maxHealth = MathUtils.max(1f, baseHealth * healthMult);
        this.attackPower = MathUtils.max(0f, baseAttack * attackMult);
        this.defense = MathUtils.max(0f, baseDefense * defenseMult);
        this.moveSpeed = MathUtils.max(0f, baseSpeed * speedMult);
        this.currentHealth = MathUtils.clamp(currentHealth, 0f, maxHealth);
    }

    // === Getters / Setters ===

    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = MathUtils.max(1f, maxHealth); }
    public float getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(float currentHealth) { this.currentHealth = MathUtils.clamp(currentHealth, 0f, maxHealth); }
    public float getAttackPower() { return attackPower; }
    public void setAttackPower(float attackPower) { this.attackPower = MathUtils.max(0f, attackPower); }
    public float getDefense() { return defense; }
    public void setDefense(float defense) { this.defense = MathUtils.max(0f, defense); }
    public float getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(float moveSpeed) { this.moveSpeed = MathUtils.max(0f, moveSpeed); }
    public float getCriticalRate() { return criticalRate; }
    public void setCriticalRate(float criticalRate) { this.criticalRate = MathUtils.clamp01(criticalRate); }
    public float getCriticalDamageMultiplier() { return criticalDamageMultiplier; }
    public void setCriticalDamageMultiplier(float criticalDamageMultiplier) { this.criticalDamageMultiplier = criticalDamageMultiplier; }
    public float getMaxEnergy() { return maxEnergy; }
    public void setMaxEnergy(float maxEnergy) { this.maxEnergy = MathUtils.max(0f, maxEnergy); }
    public float getCurrentEnergy() { return currentEnergy; }
    public void setCurrentEnergy(float currentEnergy) { this.currentEnergy = MathUtils.clamp(currentEnergy, 0f, maxEnergy); }
    public float getDamageTakenMultiplier() { return damageTakenMultiplier; }
    public void setDamageTakenMultiplier(float damageTakenMultiplier) { this.damageTakenMultiplier = MathUtils.max(0f, damageTakenMultiplier); }

    @Override
    public String toString() {
        return String.format("HP:%.0f/%.0f ATK:%.0f DEF:%.0f SPD:%.1f", currentHealth, maxHealth, getEffectiveAttackPower(), getEffectiveDefense(), getEffectiveMoveSpeed());
    }
}
