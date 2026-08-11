package com.actiongame.server.anticheat;

/**
 * 作弊严重度等级
 */
public enum CheatSeverity {
    /** 轻微: 可能是网络抖动, 仅记录 */
    LOW(1),
    /** 中等: 疑似作弊, 警告+回滚操作 */
    MEDIUM(2),
    /** 严重: 明确作弊, 踢出房间 */
    HIGH(3),
    /** 极其严重: 恶意作弊, 封禁 */
    CRITICAL(4);

    private final int level;

    CheatSeverity(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public int getScore() {
        return level * 10;
    }
}
