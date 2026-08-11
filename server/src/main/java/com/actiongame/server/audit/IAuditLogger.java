package com.actiongame.server.audit;

/**
 * 审计日志接口 (Phase 11 日志模块预留)
 * 记录关键操作: 登录/登出/战斗开始/结束/异常/反作弊触发
 */
public interface IAuditLogger {

    void logLogin(String playerId, boolean success, String detail);

    void logLogout(String playerId, String reason);

    void logBattleStart(String roomId, int playerCount);

    void logBattleEnd(String roomId, int result, long totalFrames);

    void logCheatDetected(String playerId, String cheatType, String detail);

    void logError(String module, String error, Throwable cause);
}
