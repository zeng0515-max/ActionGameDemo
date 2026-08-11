package com.actiongame.server.llm;

import com.actiongame.server.anticheat.CheatIncident;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗上下文 — 传递给 LLM 的数据包
 */
public class BattleContext {
    private String roomId;
    private long totalFrames;
    private long durationMs;
    private int playerCount;
    private int monsterCount;
    private int bossCount;

    // 玩家表现
    private float avgPlayerHpPercent;
    private int totalAttacks;
    private int totalHits;
    private float totalDamage;
    private int maxCombo;

    // 怪物表现
    private int monstersKilled;
    private float avgMonsterHpPercent;

    // 作弊事件
    private List<CheatIncident> cheatIncidents = new ArrayList<>();

    // 战斗结果
    private int battleResult; // 0=ongoing, 1=victory, 2=defeat
    private String resultDetail;

    public BattleContext() {}

    public BattleContext(String roomId, long totalFrames, long durationMs) {
        this.roomId = roomId;
        this.totalFrames = totalFrames;
        this.durationMs = durationMs;
    }

    // === Getters/Setters ===

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public long getTotalFrames() { return totalFrames; }
    public void setTotalFrames(long totalFrames) { this.totalFrames = totalFrames; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    public int getMonsterCount() { return monsterCount; }
    public void setMonsterCount(int monsterCount) { this.monsterCount = monsterCount; }
    public int getBossCount() { return bossCount; }
    public void setBossCount(int bossCount) { this.bossCount = bossCount; }
    public float getAvgPlayerHpPercent() { return avgPlayerHpPercent; }
    public void setAvgPlayerHpPercent(float avgPlayerHpPercent) { this.avgPlayerHpPercent = avgPlayerHpPercent; }
    public int getTotalAttacks() { return totalAttacks; }
    public void setTotalAttacks(int totalAttacks) { this.totalAttacks = totalAttacks; }
    public int getTotalHits() { return totalHits; }
    public void setTotalHits(int totalHits) { this.totalHits = totalHits; }
    public float getTotalDamage() { return totalDamage; }
    public void setTotalDamage(float totalDamage) { this.totalDamage = totalDamage; }
    public int getMaxCombo() { return maxCombo; }
    public void setMaxCombo(int maxCombo) { this.maxCombo = maxCombo; }
    public int getMonstersKilled() { return monstersKilled; }
    public void setMonstersKilled(int monstersKilled) { this.monstersKilled = monstersKilled; }
    public float getAvgMonsterHpPercent() { return avgMonsterHpPercent; }
    public void setAvgMonsterHpPercent(float avgMonsterHpPercent) { this.avgMonsterHpPercent = avgMonsterHpPercent; }
    public List<CheatIncident> getCheatIncidents() { return cheatIncidents; }
    public void setCheatIncidents(List<CheatIncident> cheatIncidents) { this.cheatIncidents = cheatIncidents; }
    public int getBattleResult() { return battleResult; }
    public void setBattleResult(int battleResult) { this.battleResult = battleResult; }
    public String getResultDetail() { return resultDetail; }
    public void setResultDetail(String resultDetail) { this.resultDetail = resultDetail; }

    /**
     * 计算命中率
     */
    public float getHitRate() {
        return totalAttacks > 0 ? (float) totalHits / totalAttacks : 0f;
    }

    /**
     * 计算DPS (每秒伤害)
     */
    public float getDPS() {
        float durationSec = durationMs / 1000f;
        return durationSec > 0 ? totalDamage / durationSec : 0f;
    }

    /**
     * 战斗时长 (秒)
     */
    public float getDurationSeconds() {
        return durationMs / 1000f;
    }
}
