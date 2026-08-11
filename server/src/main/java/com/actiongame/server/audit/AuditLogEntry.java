package com.actiongame.server.audit;

/**
 * 结构化审计日志条目
 */
public class AuditLogEntry {
    private final long timestamp;
    private final AuditLogType type;
    private final String playerId;
    private final String roomId;
    private final String detail;
    private final String module;

    public AuditLogEntry(AuditLogType type, String playerId, String roomId, String detail, String module) {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.playerId = playerId;
        this.roomId = roomId;
        this.detail = detail;
        this.module = module;
    }

    public long getTimestamp() { return timestamp; }
    public AuditLogType getType() { return type; }
    public String getPlayerId() { return playerId; }
    public String getRoomId() { return roomId; }
    public String getDetail() { return detail; }
    public String getModule() { return module; }

    /**
     * 序列化为 JSON 格式 (用于文件持久化)
     */
    public String toJson() {
        return String.format(
            "{\"ts\":%d,\"type\":\"%s\",\"player\":\"%s\",\"room\":\"%s\",\"module\":\"%s\",\"detail\":\"%s\"}",
            timestamp, type, escape(playerId), escape(roomId), escape(module), escape(detail)
        );
    }

    @Override
    public String toString() {
        return String.format("[AUDIT][%s] player=%s room=%s module=%s detail=%s",
            type, playerId, roomId, module, detail);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
