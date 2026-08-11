package com.actiongame.server.llm;

/**
 * 难度调整建议
 */
public class DifficultySuggestion {

    public enum Action {
        NO_CHANGE,         // 保持当前难度
        INCREASE_HEALTH,   // 提升怪物血量
        INCREASE_DAMAGE,   // 提升怪物攻击力
        INCREASE_SPEED,    // 提升怪物移速
        SPAWN_MORE,        // 增加怪物数量
        DECREASE_HEALTH,   // 降低怪物血量
        DECREASE_DAMAGE,   // 降低怪物攻击力
        DECREASE_SPEED     // 降低怪物移速
    }

    private final Action action;
    private final float adjustmentPercent;
    private final String reason;

    public DifficultySuggestion(Action action, float adjustmentPercent, String reason) {
        this.action = action;
        this.adjustmentPercent = adjustmentPercent;
        this.reason = reason;
    }

    public Action getAction() { return action; }
    public float getAdjustmentPercent() { return adjustmentPercent; }
    public String getReason() { return reason; }

    @Override
    public String toString() {
        return String.format("DifficultySuggestion{action=%s, pct=%.0f%%, reason=%s}",
            action, adjustmentPercent * 100, reason);
    }

    public static DifficultySuggestion noChange(String reason) {
        return new DifficultySuggestion(Action.NO_CHANGE, 0f, reason);
    }
}
