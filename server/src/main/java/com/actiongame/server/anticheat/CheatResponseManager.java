package com.actiongame.server.anticheat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作弊响应管理器 — 根据累计违规分数决定处罚
 *
 * 处罚阶梯:
 *  0-9:   正常 (无处罚)
 * 10-19:  警告 (日志记录)
 * 20-39:  回滚操作 (拒绝当前请求)
 * 40-79:  踢出房间 (断开连接)
 * 80+:    封禁 (标记玩家)
 */
public class CheatResponseManager {
    private static final Logger log = LoggerFactory.getLogger(CheatResponseManager.class);

    public enum PunishAction {
        NONE,           // 无处罚
        WARN,           // 警告
        ROLLBACK,       // 回滚操作
        KICK,           // 踢出房间
        BAN             // 封禁
    }

    /** playerId → 累计违规分数 */
    private final Map<String, AtomicInteger> violationScores = new ConcurrentHashMap<>();

    /** playerId → 封禁状态 */
    private final Map<String, Boolean> bannedPlayers = new ConcurrentHashMap<>();

    /**
     * 提交作弊事件, 返回应执行的处罚
     */
    public PunishAction submitIncident(CheatIncident incident) {
        if (incident == null) return PunishAction.NONE;

        String playerId = incident.getPlayerId();
        int scoreToAdd = incident.getSeverity().getScore();
        int totalScore = violationScores
            .computeIfAbsent(playerId, k -> new AtomicInteger(0))
            .addAndGet(scoreToAdd);

        log.warn("[CheatResponse] {} total score={} (+{} for {})",
            playerId, totalScore, scoreToAdd, incident.getCheatType());

        // 封禁检查优先
        if (bannedPlayers.getOrDefault(playerId, false)) {
            return PunishAction.BAN;
        }

        if (totalScore >= AntiCheatConfig.BAN_THRESHOLD) {
            bannedPlayers.put(playerId, true);
            log.error("[CheatResponse] Player {} BANNED (score={})", playerId, totalScore);
            return PunishAction.BAN;
        }

        if (totalScore >= AntiCheatConfig.KICK_THRESHOLD) {
            log.error("[CheatResponse] Player {} KICKED (score={})", playerId, totalScore);
            return PunishAction.KICK;
        }

        if (totalScore >= AntiCheatConfig.ROLLBACK_THRESHOLD) {
            return PunishAction.ROLLBACK;
        }

        if (totalScore >= AntiCheatConfig.WARN_THRESHOLD) {
            return PunishAction.WARN;
        }

        return PunishAction.NONE;
    }

    /**
     * 获取玩家违规分数
     */
    public int getViolationScore(String playerId) {
        AtomicInteger score = violationScores.get(playerId);
        return score != null ? score.get() : 0;
    }

    /**
     * 检查玩家是否被封禁
     */
    public boolean isBanned(String playerId) {
        return bannedPlayers.getOrDefault(playerId, false);
    }

    /**
     * 违规分数衰减 (定期调用)
     */
    public void decayScores() {
        violationScores.forEach((playerId, score) -> {
            int current = score.get();
            if (current > 0) {
                int decayed = Math.max(0, current - 1);
                score.compareAndSet(current, decayed);
            }
        });
    }

    /**
     * 清除玩家记录 (玩家离开)
     */
    public void clearPlayer(String playerId) {
        violationScores.remove(playerId);
    }
}
