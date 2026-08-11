package com.actiongame.server.config;

/**
 * 角色配置 (对应Unity CharacterData ScriptableObject)
 * 从JSON加载, 服务端权威
 */
public class CharacterConfig {
    private String characterName;
    private int level;
    private float maxHealth;
    private float attackPower;
    private float defense;
    private float moveSpeed;
    private float attackSpeed;
    private float criticalRate;
    private float criticalDamage;

    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }
    public float getAttackPower() { return attackPower; }
    public void setAttackPower(float attackPower) { this.attackPower = attackPower; }
    public float getDefense() { return defense; }
    public void setDefense(float defense) { this.defense = defense; }
    public float getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(float moveSpeed) { this.moveSpeed = moveSpeed; }
    public float getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(float attackSpeed) { this.attackSpeed = attackSpeed; }
    public float getCriticalRate() { return criticalRate; }
    public void setCriticalRate(float criticalRate) { this.criticalRate = criticalRate; }
    public float getCriticalDamage() { return criticalDamage; }
    public void setCriticalDamage(float criticalDamage) { this.criticalDamage = criticalDamage; }

    @Override
    public String toString() {
        return "CharacterConfig{name=" + characterName + ", hp=" + maxHealth + ", atk=" + attackPower + "}";
    }
}
