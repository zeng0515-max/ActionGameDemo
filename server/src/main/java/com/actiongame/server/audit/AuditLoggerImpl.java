package com.actiongame.server.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

/**
 * 审计日志实现 — 结构化日志 + 文件持久化 + 内存查询 + 指标统计
 *
 * 三层存储:
 * 1. SLF4J 日志 (控制台 + rolling file, 由 logback.xml 配置)
 * 2. 内存环形缓冲 (最近1000条, 支持查询)
 * 3. JSON 行文件持久化 (logs/audit-yyyy-MM-dd.jsonl, 按天滚动)
 */
public class AuditLoggerImpl implements IAuditLogger {
    private static final Logger log = LoggerFactory.getLogger(AuditLoggerImpl.class);

    private static final int MAX_BUFFER_SIZE = 1000;
    private final ConcurrentLinkedDeque<AuditLogEntry> buffer = new ConcurrentLinkedDeque<>();

    /** 文件持久化目录 */
    private final Path auditLogDir;
    private volatile LocalDate currentDate;
    private volatile Path currentLogFile;

    /** 服务端指标 */
    private final ServerMetrics metrics = new ServerMetrics();

    public AuditLoggerImpl() {
        this(Path.of("logs"));
    }

    public AuditLoggerImpl(Path logDir) {
        this.auditLogDir = logDir.resolve("audit");
        try {
            Files.createDirectories(this.auditLogDir);
        } catch (IOException e) {
            log.error("Failed to create audit log directory: {}", this.auditLogDir, e);
        }
        this.currentDate = LocalDate.now();
        this.currentLogFile = this.auditLogDir.resolve("audit-" + currentDate + ".jsonl");
    }

    // === IAuditLogger 实现 ===

    @Override
    public void logLogin(String playerId, boolean success, String detail) {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.LOGIN, playerId, null, detail, "LoginHandler");
        writeEntry(entry, success);
        if (success) metrics.onLoginSuccess();
        else metrics.onLoginFailure();
    }

    @Override
    public void logLogout(String playerId, String reason) {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.LOGOUT, playerId, null, reason, "ConnectionManager");
        writeEntry(entry, true);
    }

    @Override
    public void logBattleStart(String roomId, int playerCount) {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.BATTLE_START, null, roomId,
            "players=" + playerCount, "BattleRoom");
        writeEntry(entry, true);
        metrics.onBattleStart();
    }

    @Override
    public void logBattleEnd(String roomId, int result, long totalFrames) {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.BATTLE_END, null, roomId,
            "result=" + result + " frames=" + totalFrames, "BattleRoom");
        writeEntry(entry, true);
        metrics.onBattleEnd(totalFrames);
    }

    @Override
    public void logCheatDetected(String playerId, String cheatType, String detail) {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.CHEAT_DETECTED, playerId, null,
            cheatType + ": " + detail, "CheatDetector");
        writeEntry(entry, false);
        metrics.onCheatIncident();
    }

    @Override
    public void logError(String module, String error, Throwable cause) {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.ERROR, null, null,
            error + (cause != null ? " cause=" + cause.getMessage() : ""),
            module);
        writeEntry(entry, false);
        metrics.onError(module);
    }

    // === 扩展 API ===

    public void logPlayerJoinRoom(String playerId, String roomId, int entityId) {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.PLAYER_JOIN_ROOM, playerId, roomId,
            "entityId=" + entityId, "JoinRoomHandler");
        writeEntry(entry, true);
    }

    public void logPlayerAction(String playerId, int actionType, String roomId) {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.PLAYER_ACTION, playerId, roomId,
            "action=" + actionType, "PlayerActionHandler");
        writeEntry(entry, true);
        metrics.onPlayerAction();
    }

    public void logSystem(String detail) {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.SYSTEM, "SYSTEM", null, detail, "Bootstrap");
        writeEntry(entry, true);
    }

    // === 查询 API ===

    /**
     * 获取最近的审计日志 (只读副本)
     */
    public List<AuditLogEntry> getRecentEntries() {
        return new ArrayList<>(buffer);
    }

    /**
     * 按类型筛选日志
     */
    public List<AuditLogEntry> getEntriesByType(AuditLogType type) {
        List<AuditLogEntry> result = new ArrayList<>();
        for (AuditLogEntry e : buffer) {
            if (e.getType() == type) result.add(e);
        }
        return result;
    }

    /**
     * 按玩家筛选日志
     */
    public List<AuditLogEntry> getEntriesByPlayer(String playerId) {
        List<AuditLogEntry> result = new ArrayList<>();
        for (AuditLogEntry e : buffer) {
            if (playerId.equals(e.getPlayerId())) result.add(e);
        }
        return result;
    }

    /**
     * 获取指定时间段内的日志
     */
    public List<AuditLogEntry> getEntriesInRange(long fromTs, long toTs) {
        List<AuditLogEntry> result = new ArrayList<>();
        for (AuditLogEntry e : buffer) {
            if (e.getTimestamp() >= fromTs && e.getTimestamp() <= toTs) result.add(e);
        }
        return result;
    }

    /**
     * 获取服务端指标
     */
    public ServerMetrics getMetrics() {
        return metrics;
    }

    /**
     * 获取指标摘要
     */
    public String getMetricsSummary() {
        return metrics.getSummary();
    }

    /**
     * 清空内存缓冲
     */
    public void clear() {
        buffer.clear();
    }

    /**
     * 清理过期审计日志文件 (超过 N 天)
     */
    public int cleanupOldAuditFiles(int retentionDays) {
        int deleted = 0;
        if (!Files.exists(auditLogDir)) return 0;

        LocalDate cutoff = LocalDate.now().minusDays(retentionDays);
        try (Stream<Path> paths = Files.list(auditLogDir)) {
            var expired = paths
                .filter(p -> p.getFileName().toString().startsWith("audit-"))
                .filter(p -> {
                    String name = p.getFileName().toString();
                    // audit-yyyy-MM-dd.jsonl
                    try {
                        String dateStr = name.substring(6, 16); // yyyy-MM-dd
                        LocalDate fileDate = LocalDate.parse(dateStr);
                        return fileDate.isBefore(cutoff);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();

            for (Path file : expired) {
                try {
                    Files.delete(file);
                    deleted++;
                } catch (IOException e) {
                    log.warn("Failed to delete audit log: {}", file);
                }
            }
        } catch (IOException e) {
            log.error("Failed to cleanup audit logs", e);
        }
        return deleted;
    }

    // === 内部方法 ===

    private void writeEntry(AuditLogEntry entry, boolean success) {
        // 1. SLF4J 输出
        if (entry.getType() == AuditLogType.ERROR) {
            log.error("{}", entry);
        } else if (entry.getType() == AuditLogType.CHEAT_DETECTED) {
            log.warn("{}", entry);
        } else {
            log.info("{}", entry);
        }

        // 2. 内存缓冲
        buffer.addLast(entry);
        while (buffer.size() > MAX_BUFFER_SIZE) {
            buffer.pollFirst();
        }

        // 3. 文件持久化 (JSON 行)
        persistToFile(entry);
    }

    private void persistToFile(AuditLogEntry entry) {
        try {
            // 检查日期变化 (按天滚动)
            LocalDate today = LocalDate.now();
            if (!today.equals(currentDate)) {
                currentDate = today;
                currentLogFile = auditLogDir.resolve("audit-" + today + ".jsonl");
            }

            String jsonLine = entry.toJson() + System.lineSeparator();
            Files.writeString(currentLogFile, jsonLine,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Failed to persist audit log entry", e);
        }
    }
}
