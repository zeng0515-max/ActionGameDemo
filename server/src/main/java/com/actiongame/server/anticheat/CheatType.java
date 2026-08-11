package com.actiongame.server.anticheat;

/**
 * 作弊类型枚举
 */
public enum CheatType {
    /** 移动速度超限 */
    SPEED_HACK("移动速度异常"),
    /** 瞬移 (单帧位移过大) */
    TELEPORT("瞬移"),
    /** 攻击频率超限 */
    ATTACK_RATE_HACK("攻击频率异常"),
    /** 伤害数值异常 */
    DAMAGE_HACK("伤害数值异常"),
    /** 属性越界 */
    STAT_OVERRIDE("属性越界"),
    /** 不可能的状态操作 (死亡后操作等) */
    IMPOSSIBLE_ACTION("非法操作"),
    /** 回放统计异常 (DPS/命中率异常) */
    ABNORMAL_PATTERN("行为模式异常");

    private final String description;

    CheatType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
