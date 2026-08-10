package com.actiongame.server.persistence;

public class MatchResult {
    private final String roomId;
    private final long startTimeMs;
    private final long endTimeMs;
    private final long totalFrames;
    private final int result;
    private final int playerCount;
    private final int monsterCount;
    private final int cheatScore;

    public MatchResult(String roomId, long startTimeMs, long endTimeMs, long totalFrames,
                       int result, int playerCount, int monsterCount, int cheatScore) {
        this.roomId = roomId;
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.totalFrames = totalFrames;
        this.result = result;
        this.playerCount = playerCount;
        this.monsterCount = monsterCount;
        this.cheatScore = cheatScore;
    }

    public String getRoomId() { return roomId; }
    public long getStartTimeMs() { return startTimeMs; }
    public long getEndTimeMs() { return endTimeMs; }
    public long getTotalFrames() { return totalFrames; }
    public int getResult() { return result; }
    public int getPlayerCount() { return playerCount; }
    public int getMonsterCount() { return monsterCount; }
    public int getCheatScore() { return cheatScore; }
}
