package com.actiongame.server.audit;

/**
 * 审计日志类型
 */
public enum AuditLogType {
    LOGIN,
    LOGOUT,
    BATTLE_START,
    BATTLE_END,
    CHEAT_DETECTED,
    PLAYER_JOIN_ROOM,
    PLAYER_LEAVE_ROOM,
    PLAYER_ACTION,
    ERROR,
    SYSTEM
}
