package com.actiongame.server.room;

import java.util.List;

public record RoomSnapshot(
    String roomId,
    String status,
    long currentFrameIndex,
    long startTimeMs,
    int nextEntityId,
    List<PlayerSnapshot> players,
    List<MonsterSnapshot> monsters
) {

    public record PlayerSnapshot(
        int entityId,
        String playerId,
        int configId,
        int level,
        float currentExp,
        int availableSkillPoints,
        String state,
        float posX,
        float posY,
        float posZ,
        float rotX,
        float rotY,
        float rotZ,
        float rotW,
        String element,
        float shield,
        boolean invincible,
        boolean dead,
        int targetEntityId,
        int comboStep,
        StatsSnapshot stats,
        List<BuffSnapshot> buffs
    ) {}

    public record MonsterSnapshot(
        int entityId,
        int configId,
        boolean boss,
        String state,
        float posX,
        float posY,
        float posZ,
        float rotX,
        float rotY,
        float rotZ,
        float rotW,
        String element,
        float shield,
        boolean invincible,
        boolean dead,
        int targetEntityId,
        int comboStep,
        int monsterType,
        float detectionRange,
        float attackRange,
        float attackCooldown,
        float patrolRadius,
        float fleeThreshold,
        String enemyName,
        StatsSnapshot stats,
        List<BuffSnapshot> buffs
    ) {}

    public record StatsSnapshot(
        float maxHealth,
        float currentHealth,
        float attackPower,
        float defense,
        float moveSpeed,
        float criticalRate,
        float criticalDamageMultiplier,
        float maxEnergy,
        float currentEnergy
    ) {}

    public record BuffSnapshot(
        int buffId,
        String buffName,
        String buffType,
        float duration,
        float remainingTime,
        int stacks,
        int maxStacks,
        String stackingRule,
        float tickInterval,
        float tickValuePercent,
        String attributeType,
        float attributeModifier,
        float shieldValue,
        String relatedElement,
        int targetEntityId,
        int sourceEntityId,
        boolean active
    ) {}
}
