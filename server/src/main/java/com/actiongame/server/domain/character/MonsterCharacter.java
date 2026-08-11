package com.actiongame.server.domain.character;

/**
 * 怪物角色 (服务端权威)
 */
public class MonsterCharacter extends Character {

    private int monsterType;
    private float detectionRange = 10f;
    private float attackRange = 2f;
    private float attackCooldown = 3f;
    private float patrolRadius = 5f;
    private float fleeThreshold = 0.2f;

    public MonsterCharacter(int entityId, int configId, CharacterStats stats) {
        super(entityId, configId, stats);
    }

    public int getMonsterType() { return monsterType; }
    public void setMonsterType(int monsterType) { this.monsterType = monsterType; }
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
}
