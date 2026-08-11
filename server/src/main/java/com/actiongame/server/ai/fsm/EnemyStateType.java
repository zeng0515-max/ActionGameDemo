package com.actiongame.server.ai.fsm;

/**
 * 敌人状态类型 (对应Unity EnemyStateType)
 * 注意: 无Attack状态 — 用户要求敌人不攻击玩家
 */
public enum EnemyStateType {
    IDLE,
    PATROL,
    CHASE,
    FLEE,
    HURT,
    DEAD
}
