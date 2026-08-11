package com.actiongame.server.anticheat;

/**
 * 作弊事件记录
 */
public class CheatIncident {
    private final String playerId;
    private final int entityId;
    private final CheatType cheatType;
    private final CheatSeverity severity;
    private final long timestamp;
    private final String detail;

    public CheatIncident(String playerId, int entityId, CheatType cheatType,
                         CheatSeverity severity, long timestamp, String detail) {
        this.playerId = playerId;
        this.entityId = entityId;
        this.cheatType = cheatType;
        this.severity = severity;
        this.timestamp = timestamp;
        this.detail = detail;
    }

    public String getPlayerId() { return playerId; }
    public int getEntityId() { return entityId; }
    public CheatType getCheatType() { return cheatType; }
    public CheatSeverity getSeverity() { return severity; }
    public long getTimestamp() { return timestamp; }
    public String getDetail() { return detail; }

    @Override
    public String toString() {
        return String.format("[%s] %s entityId=%d type=%s severity=%s detail=%s",
            timestamp, playerId, entityId, cheatType, severity, detail);
    }
}
