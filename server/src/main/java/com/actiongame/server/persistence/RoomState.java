package com.actiongame.server.persistence;

public class RoomState {
    private String roomId;
    private String status;
    private int playerCount;
    private int monsterCount;
    private long currentFrameIndex;
    private long updatedAtMs;

    public RoomState() {}

    public RoomState(String roomId, String status, int playerCount, int monsterCount,
                     long currentFrameIndex, long updatedAtMs) {
        this.roomId = roomId;
        this.status = status;
        this.playerCount = playerCount;
        this.monsterCount = monsterCount;
        this.currentFrameIndex = currentFrameIndex;
        this.updatedAtMs = updatedAtMs;
    }

    public String getRoomId() { return roomId; }
    public String getStatus() { return status; }
    public int getPlayerCount() { return playerCount; }
    public int getMonsterCount() { return monsterCount; }
    public long getCurrentFrameIndex() { return currentFrameIndex; }
    public long getUpdatedAtMs() { return updatedAtMs; }
}
