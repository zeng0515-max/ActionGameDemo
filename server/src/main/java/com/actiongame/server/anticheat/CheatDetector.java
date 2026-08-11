package com.actiongame.server.anticheat;

import com.actiongame.server.domain.character.Character;
import com.actiongame.server.replay.BattleRecording;
import com.actiongame.server.util.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 反作弊检测器 — 三层架构实现
 *
 * Layer 1 (实时): 移动速度/攻击频率/瞬移检测 — 在 submitPlayerAction 时调用
 * Layer 2 (逻辑): 伤害/属性范围校验 — 在 executeAttack 时调用
 * Layer 3 (离线): 回放统计分析 — 在战斗结束时调用
 */
public class CheatDetector implements ICheatDetector {
    private static final Logger log = LoggerFactory.getLogger(CheatDetector.class);

    /** entityId → 行为追踪器 */
    private final Map<Integer, PlayerBehaviorTracker> trackers = new ConcurrentHashMap<>();

    /** 累计作弊事件列表 (线程安全, 可能被 Netty IO 线程和帧调度线程同时访问) */
    private final List<CheatIncident> incidents = new CopyOnWriteArrayList<>();

    /** 帧间隔 (秒) */
    private final float deltaTime;

    public CheatDetector(float deltaTime) {
        this.deltaTime = deltaTime;
    }

    // === Layer 1: 实时行为检测 ===

    @Override
    public boolean checkRealtime(int entityId, int actionType, float moveX, float moveZ, long timestamp, float maxMoveSpeed) {
        PlayerBehaviorTracker tracker = getOrCreateTracker(entityId);

        // 动作类型: 1=MOVE, 2=JUMP, 3=ATTACK, 4=SKILL, 5=ULTIMATE, 6=DODGE
        switch (actionType) {
            case 1 -> { // MOVE
                return checkMoveSpeed(entityId, tracker, moveX, moveZ, maxMoveSpeed);
            }
            case 3, 4, 5 -> { // ATTACK / SKILL / ULTIMATE
                return checkAttackRate(entityId, tracker, timestamp);
            }
            case 6 -> { // DODGE
                return checkDodgeRate(entityId, tracker, timestamp);
            }
        }
        return false;
    }

    /**
     * 移动速度检测
     */
    private boolean checkMoveSpeed(int entityId, PlayerBehaviorTracker tracker, float moveX, float moveZ, float maxMoveSpeed) {
        // 输入向量长度 > 1 表示客户端可能修改了输入 (正常值为 -1~1)
        float inputMag = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (inputMag > 1.5f) {
            recordIncident(entityId, CheatType.SPEED_HACK, CheatSeverity.MEDIUM,
                String.format("输入向量异常: %.2f (max=1.0)", inputMag));
            return true;
        }

        // 检查理论最大速度: 输入长度 × 角色实际移速
        float theoreticalSpeed = inputMag * maxMoveSpeed;
        if (theoreticalSpeed > AntiCheatConfig.MAX_MOVE_SPEED) {
            recordIncident(entityId, CheatType.SPEED_HACK, CheatSeverity.HIGH,
                String.format("理论速度 %.1f 超过上限 %.1f (移速=%.1f)", theoreticalSpeed, AntiCheatConfig.MAX_MOVE_SPEED, maxMoveSpeed));
            return true;
        }
        return false;
    }

    /**
     * 攻击频率检测
     */
    private boolean checkAttackRate(int entityId, PlayerBehaviorTracker tracker, long timestamp) {
        // 先检查频率 (在记录当前攻击之前)
        int recentAttacks = tracker.getAttackCountInLastSecond(timestamp);

        if (recentAttacks >= AntiCheatConfig.MAX_ATTACKS_PER_SECOND) {
            // 不记录被拒绝的攻击, 避免计数器膨胀导致后续合法攻击也被拒绝
            recordIncident(entityId, CheatType.ATTACK_RATE_HACK, CheatSeverity.HIGH,
                String.format("1秒内攻击 %d 次 (上限 %.0f)", recentAttacks, AntiCheatConfig.MAX_ATTACKS_PER_SECOND));
            return true;
        }

        long lastAttack = tracker.getLastAttackTimestamp();
        if (lastAttack > 0) {
            float interval = (timestamp - lastAttack) / 1000f;
            if (interval < AntiCheatConfig.MIN_ATTACK_INTERVAL) {
                // 不记录被拒绝的攻击
                recordIncident(entityId, CheatType.ATTACK_RATE_HACK, CheatSeverity.MEDIUM,
                    String.format("攻击间隔 %.3fs (最小 %.3fs)", interval, AntiCheatConfig.MIN_ATTACK_INTERVAL));
                return true;
            }
        }

        // 检查通过后才记录
        tracker.recordAttack(timestamp);
        return false;
    }

    /**
     * 闪避频率检测 (冷却检测)
     */
    private boolean checkDodgeRate(int entityId, PlayerBehaviorTracker tracker, long timestamp) {
        long lastDodge = tracker.getLastDodgeTimestamp();
        if (lastDodge > 0) {
            float interval = (timestamp - lastDodge) / 1000f;
            if (interval < 0.3f) { // 闪避最小冷却0.3秒
                recordIncident(entityId, CheatType.ATTACK_RATE_HACK, CheatSeverity.LOW,
                    String.format("闪避间隔 %.3fs 过短", interval));
                return true;
            }
        }
        tracker.recordDodge(timestamp);
        return false;
    }

    // === Layer 2: 逻辑校验 ===

    @Override
    public boolean checkLogic(Character attacker, Character target, float damage) {
        if (attacker == null || target == null) return false;

        // 伤害值越界
        if (damage > AntiCheatConfig.MAX_SINGLE_DAMAGE) {
            recordIncident(attacker.getEntityId(), CheatType.DAMAGE_HACK, CheatSeverity.CRITICAL,
                String.format("单次伤害 %.0f 超过上限 %.0f", damage, AntiCheatConfig.MAX_SINGLE_DAMAGE));
            return true;
        }

        if (damage < 0) {
            recordIncident(attacker.getEntityId(), CheatType.DAMAGE_HACK, CheatSeverity.HIGH,
                String.format("负伤害 %.1f (治疗攻击?)", damage));
            return true;
        }

        // 攻击者属性越界
        float atk = attacker.getStats().getEffectiveAttackPower();
        if (atk > AntiCheatConfig.MAX_ATTACK_POWER) {
            recordIncident(attacker.getEntityId(), CheatType.STAT_OVERRIDE, CheatSeverity.HIGH,
                String.format("攻击力 %.0f 超过上限 %.0f", atk, AntiCheatConfig.MAX_ATTACK_POWER));
            return true;
        }

        float def = target.getStats().getEffectiveDefense();
        if (def > AntiCheatConfig.MAX_DEFENSE) {
            recordIncident(target.getEntityId(), CheatType.STAT_OVERRIDE, CheatSeverity.HIGH,
                String.format("防御力 %.0f 超过上限 %.0f", def, AntiCheatConfig.MAX_DEFENSE));
            return true;
        }

        float hp = target.getStats().getMaxHealth();
        if (hp > AntiCheatConfig.MAX_HEALTH) {
            recordIncident(target.getEntityId(), CheatType.STAT_OVERRIDE, CheatSeverity.HIGH,
                String.format("最大生命值 %.0f 超过上限 %.0f", hp, AntiCheatConfig.MAX_HEALTH));
            return true;
        }

        // 伤害/攻击力 比率异常 (伤害远超攻击力×技能倍率上限)
        if (atk > 0 && damage > atk * AntiCheatConfig.MAX_SKILL_MULTIPLIER) {
            recordIncident(attacker.getEntityId(), CheatType.DAMAGE_HACK, CheatSeverity.MEDIUM,
                String.format("伤害 %.1f / 攻击力 %.1f = %.1f倍 (上限 %.1f倍)",
                    damage, atk, damage / atk, AntiCheatConfig.MAX_SKILL_MULTIPLIER));
            return true;
        }

        return false;
    }

    // === Layer 3: 回放分析 ===

    @Override
    public int analyzeReplay(BattleRecording recording) {
        if (recording == null || recording.getFrames().isEmpty()) return 0;

        int totalAttacks = 0;
        long firstTimestamp = 0;
        long lastTimestamp = 0;

        for (BattleRecording.FrameInput frame : recording.getFrames()) {
            for (BattleRecording.PlayerActionRecord action : frame.getActions()) {
                if (action.getActionType() == 3 || action.getActionType() == 4 ||
                    action.getActionType() == 5) {
                    totalAttacks++;
                }
                if (firstTimestamp == 0) firstTimestamp = frame.getTimestampMs();
                lastTimestamp = frame.getTimestampMs();
            }
        }

        if (totalAttacks < AntiCheatConfig.REPLAY_MIN_ATTACKS) return 0;

        float durationSeconds = (lastTimestamp - firstTimestamp) / 1000f;
        if (durationSeconds <= 0) return 0;

        float attacksPerSecond = totalAttacks / durationSeconds;

        int suspiciousScore = 0;

        // DPS 异常 (攻击频率超过物理上限)
        if (attacksPerSecond > AntiCheatConfig.MAX_ATTACKS_PER_SECOND) {
            suspiciousScore += 30;
            log.warn("[Layer3] Replay {} abnormal attack rate: {}/s",
                recording.getRoomId(), String.format("%.1f", attacksPerSecond));
        }

        // 移动路径异常: 检测连续相同时间戳的移动操作 (帧伪造)
        int duplicateMoveTimestamps = 0;
        long prevMoveTs = 0;
        // 移动频率异常: 单秒内移动操作过多
        int moveActionCount = 0;
        long moveWindowStart = 0;

        for (BattleRecording.FrameInput frame : recording.getFrames()) {
            for (BattleRecording.PlayerActionRecord action : frame.getActions()) {
                if (action.getActionType() == 1) { // MOVE
                    moveActionCount++;
                    if (moveWindowStart == 0) {
                        moveWindowStart = frame.getTimestampMs();
                    } else if (frame.getTimestampMs() - moveWindowStart > 1000) {
                        // 超过1秒窗口, 重置
                        moveActionCount = 1;
                        moveWindowStart = frame.getTimestampMs();
                    }

                    if (moveActionCount > AntiCheatConfig.MAX_ATTACKS_PER_SECOND * 2) {
                        // 1秒内移动操作超过上限2倍, 可能是自动脚本
                        suspiciousScore += 20;
                        log.warn("[Layer3] Replay {} abnormal move rate: {} actions in 1s",
                            recording.getRoomId(), moveActionCount);
                        moveActionCount = 0; // 避免重复加分
                    }

                    if (prevMoveTs > 0 && frame.getTimestampMs() == prevMoveTs) {
                        duplicateMoveTimestamps++;
                    }
                    prevMoveTs = frame.getTimestampMs();
                }
            }
        }

        if (duplicateMoveTimestamps > 5) {
            suspiciousScore += 30;
            log.warn("[Layer3] Replay {} has {} duplicate move timestamps",
                recording.getRoomId(), duplicateMoveTimestamps);
        }

        return suspiciousScore;
    }

    // === 公共 API ===

    /**
     * 更新玩家位置 (每帧调用, 用于追踪移动)
     */
    public void updatePlayerPosition(int entityId, Vector3 position) {
        PlayerBehaviorTracker tracker = getOrCreateTracker(entityId);
        tracker.recordMove(tracker.getLastFrameIndex() + 1, position, deltaTime);
    }

    /**
     * 获取累计违规分数
     */
    public int getTotalViolationScore() {
        return incidents.stream().mapToInt(i -> i.getSeverity().getScore()).sum();
    }

    /**
     * 获取指定玩家的违规分数
     */
    public int getPlayerViolationScore(int entityId) {
        return incidents.stream()
            .filter(i -> i.getEntityId() == entityId)
            .mapToInt(i -> i.getSeverity().getScore())
            .sum();
    }

    /**
     * 获取所有作弊事件
     */
    public List<CheatIncident> getIncidents() {
        return new ArrayList<>(incidents);
    }

    /**
     * 移除玩家追踪 (玩家离开房间)
     */
    public void removeTracker(int entityId) {
        trackers.remove(entityId);
    }

    // === 内部方法 ===

    private PlayerBehaviorTracker getOrCreateTracker(int entityId) {
        return trackers.computeIfAbsent(entityId, PlayerBehaviorTracker::new);
    }

    private void recordIncident(int entityId, CheatType type, CheatSeverity severity, String detail) {
        CheatIncident incident = new CheatIncident(
            "entity-" + entityId, entityId, type, severity, System.currentTimeMillis(), detail
        );
        incidents.add(incident);
        log.warn("[AntiCheat] {} - {} ({})", type.getDescription(), detail, severity);
    }
}
