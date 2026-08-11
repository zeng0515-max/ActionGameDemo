package com.actiongame.server.config;

/**
 * 怪物配置 (对应Unity EnemyData ScriptableObject)
 * 从JSON加载, 服务端权威
 */
public class MonsterConfig {
    private String enemyName = "Enemy";
    private float maxHealth = 50f;
    private float attackPower = 5f;
    private float defense = 2f;
    private float moveSpeed = 3f;
    private float detectionRange = 10f;
    private float attackRange = 2f;
    private float attackCooldown = 3f;
    private float patrolRadius = 5f;
    private float fleeThreshold = 0.2f;
    private float viewAngle = 90f;

    public String getEnemyName() { return enemyName; }
    public void setEnemyName(String enemyName) { this.enemyName = enemyName; }
    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }
    public float getAttackPower() { return attackPower; }
    public void setAttackPower(float attackPower) { this.attackPower = attackPower; }
    public float getDefense() { return defense; }
    public void setDefense(float defense) { this.defense = defense; }
    public float getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(float moveSpeed) { this.moveSpeed = moveSpeed; }
    public float getDetectionRange() { return detectionRange; }
    public void setDetectionRange(float detectionRange) { this.detectionRange = detectionRange; }
    public float getAttackRange() { return attackRange; }
    public void setAttackRange(float attackRange) { this.attackRange = attackRange; }
    public float getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(float attackCooldown) { this.attackCooldown = attackCooldown; }
    public float getPatrolRadius() { return patrolRadius; }
    public void setPatrolRadius(float patrolRadius) { this.patrolRadius = patrolRadius; }
    public float getFleeThreshold() { return fleeThreshold; }
    public void setFleeThreshold(float fleeThreshold) { this.fleeThreshold = fleeThreshold; }
    public float getViewAngle() { return viewAngle; }
    public void setViewAngle(float viewAngle) { this.viewAngle = viewAngle; }

    @Override
    public String toString() {
        return "MonsterConfig{name=" + enemyName + ", hp=" + maxHealth + ", atk=" + attackPower + "}";
    }
}
