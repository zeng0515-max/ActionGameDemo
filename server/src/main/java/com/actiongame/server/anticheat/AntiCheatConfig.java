package com.actiongame.server.anticheat;

/**
 * 反作弊配置常量
 * 所有阈值基于正常游戏行为的合理上限
 */
public final class AntiCheatConfig {

    private AntiCheatConfig() {}

    // === Layer 1: 实时行为检测 ===

    /** 最大移动速度 (米/秒), 正常移速~5, 含Buff加速上限~15 */
    public static final float MAX_MOVE_SPEED = 15f;

    /** 单帧最大位移 (米), 超过则判定瞬移 */
    public static final float TELEPORT_THRESHOLD = 5f;

    /** 最大攻击频率 (次/秒), 正常攻击间隔~0.3s */
    public static final float MAX_ATTACKS_PER_SECOND = 10f;

    /** 最小攻击间隔 (秒) */
    public static final float MIN_ATTACK_INTERVAL = 0.15f;

    /** 行为追踪窗口大小 (帧数, 5秒@50fps) */
    public static final int TRACKER_WINDOW_FRAMES = 250;

    // === Layer 2: 逻辑校验 ===

    /** 单次攻击最大伤害倍率 */
    public static final float MAX_SKILL_MULTIPLIER = 10f;

    /** 单次攻击最大伤害值 */
    public static final float MAX_SINGLE_DAMAGE = 10000f;

    /** 最大攻击力属性 */
    public static final float MAX_ATTACK_POWER = 999f;

    /** 最大防御力属性 */
    public static final float MAX_DEFENSE = 999f;

    /** 最大生命值 */
    public static final float MAX_HEALTH = 99999f;

    // === Layer 3: 回放分析 ===

    /** 回放DPS异常阈值 (每秒伤害超过此值) */
    public static final float REPLAY_MAX_DPS = 5000f;

    /** 回放命中率异常阈值 (超过此值判定异常) */
    public static final float REPLAY_MAX_HIT_RATE = 0.99f;

    /** 回放最小攻击次数 (低于此值不分析) */
    public static final int REPLAY_MIN_ATTACKS = 5;

    // === 处罚策略 ===

    /** 累计违规分数阈值: 警告 */
    public static final int WARN_THRESHOLD = 10;

    /** 累计违规分数阈值: 回滚操作 */
    public static final int ROLLBACK_THRESHOLD = 20;

    /** 累计违规分数阈值: 踢出房间 */
    public static final int KICK_THRESHOLD = 40;

    /** 累计违规分数阈值: 封禁 */
    public static final int BAN_THRESHOLD = 80;

    /** 违规分数衰减间隔 (帧数, 10秒) */
    public static final int SCORE_DECAY_INTERVAL = 500;
}
