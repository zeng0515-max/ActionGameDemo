package com.actiongame.server.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计日志系统单元测试
 */
class AuditLoggerImplTest {

    @TempDir
    Path tempDir;

    private AuditLoggerImpl logger;

    @BeforeEach
    void setUp() {
        logger = new AuditLoggerImpl(tempDir);
    }

    @Test
    @DisplayName("登录日志写入并查询")
    void testLogLogin() {
        logger.logLogin("player-1", true, "version=10");

        List<AuditLogEntry> entries = logger.getRecentEntries();
        assertEquals(1, entries.size());
        assertEquals(AuditLogType.LOGIN, entries.get(0).getType());
        assertEquals("player-1", entries.get(0).getPlayerId());
    }

    @Test
    @DisplayName("战斗开始/结束日志")
    void testLogBattle() {
        logger.logBattleStart("room-001", 3);
        logger.logBattleEnd("room-001", 1, 500);

        List<AuditLogEntry> entries = logger.getEntriesByType(AuditLogType.BATTLE_START);
        assertEquals(1, entries.size());
        assertEquals("room-001", entries.get(0).getRoomId());

        List<AuditLogEntry> endEntries = logger.getEntriesByType(AuditLogType.BATTLE_END);
        assertEquals(1, endEntries.size());
    }

    @Test
    @DisplayName("作弊检测日志")
    void testLogCheat() {
        logger.logCheatDetected("player-1", "SPEED_HACK", "speed=20.0");

        List<AuditLogEntry> entries = logger.getEntriesByType(AuditLogType.CHEAT_DETECTED);
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).getDetail().contains("SPEED_HACK"));
    }

    @Test
    @DisplayName("按玩家筛选日志")
    void testFilterByPlayer() {
        logger.logLogin("player-1", true, "ok");
        logger.logLogin("player-2", true, "ok");
        logger.logCheatDetected("player-1", "DAMAGE_HACK", "damage=99999");

        List<AuditLogEntry> p1Entries = logger.getEntriesByPlayer("player-1");
        assertEquals(2, p1Entries.size());

        List<AuditLogEntry> p2Entries = logger.getEntriesByPlayer("player-2");
        assertEquals(1, p2Entries.size());
    }

    @Test
    @DisplayName("按时间范围筛选")
    void testFilterByTimeRange() {
        long before = System.currentTimeMillis();
        logger.logLogin("p1", true, "test1");
        long mid = System.currentTimeMillis();
        logger.logLogin("p2", true, "test2");
        long after = System.currentTimeMillis();

        List<AuditLogEntry> range = logger.getEntriesInRange(before, mid);
        assertTrue(range.size() >= 1);
    }

    @Test
    @DisplayName("内存缓冲上限")
    void testBufferLimit() {
        for (int i = 0; i < 1500; i++) {
            logger.logLogin("player-" + i, true, "test");
        }
        List<AuditLogEntry> entries = logger.getRecentEntries();
        assertTrue(entries.size() <= 1000, "Buffer should be capped at 1000");
    }

    @Test
    @DisplayName("指标统计: 连接和登录")
    void testMetricsConnectionLogin() {
        logger.logLogin("p1", true, "ok");
        logger.logLogin("p2", false, "version mismatch");

        ServerMetrics m = logger.getMetrics();
        assertEquals(1, m.getLoginSuccess());
        assertEquals(1, m.getLoginFailures());
    }

    @Test
    @DisplayName("指标统计: 战斗")
    void testMetricsBattle() {
        logger.logBattleStart("room-1", 2);
        logger.logBattleEnd("room-1", 0, 1000);
        logger.logBattleStart("room-2", 1);
        logger.logBattleEnd("room-2", 1, 500);

        ServerMetrics m = logger.getMetrics();
        assertEquals(2, m.getBattlesStarted());
        assertEquals(2, m.getBattlesEnded());
        assertEquals(1500, m.getTotalFramesExecuted());
    }

    @Test
    @DisplayName("指标统计: 作弊")
    void testMetricsCheat() {
        logger.logCheatDetected("p1", "SPEED_HACK", "test");
        logger.logCheatDetected("p2", "DAMAGE_HACK", "test");

        ServerMetrics m = logger.getMetrics();
        assertEquals(2, m.getCheatIncidents());
    }

    @Test
    @DisplayName("指标摘要包含关键字段")
    void testMetricsSummary() {
        logger.logLogin("p1", true, "ok");
        logger.logBattleStart("room-1", 1);
        logger.logCheatDetected("p1", "SPEED_HACK", "test");

        String summary = logger.getMetricsSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("logins=1"));
        assertTrue(summary.contains("battles=1"));
        assertTrue(summary.contains("cheats=1"));
    }

    @Test
    @DisplayName("JSON 持久化文件生成")
    void testFilePersistence() {
        logger.logLogin("p1", true, "test login");

        // 检查 audit 目录下有文件
        Path auditDir = tempDir.resolve("audit");
        assertTrue(java.nio.file.Files.exists(auditDir));

        // 检查 JSONL 文件存在且有内容
        var files = auditDir.toFile().listFiles((dir, name) -> name.startsWith("audit-"));
        assertNotNull(files);
        assertTrue(files.length > 0);

        try {
            String content = java.nio.file.Files.readString(files[0].toPath());
            assertTrue(content.contains("LOGIN"));
            assertTrue(content.contains("p1"));
        } catch (Exception e) {
            fail("Failed to read audit file: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("玩家操作日志")
    void testLogPlayerAction() {
        logger.logPlayerAction("p1", 1, "room-001");

        List<AuditLogEntry> entries = logger.getEntriesByType(AuditLogType.PLAYER_ACTION);
        assertEquals(1, entries.size());
        assertEquals("p1", entries.get(0).getPlayerId());
    }

    @Test
    @DisplayName("错误日志带模块")
    void testLogError() {
        logger.logError("BattleRoom", "Frame execution failed", new RuntimeException("test"));

        List<AuditLogEntry> entries = logger.getEntriesByType(AuditLogType.ERROR);
        assertEquals(1, entries.size());
        assertEquals("BattleRoom", entries.get(0).getModule());

        ServerMetrics m = logger.getMetrics();
        assertEquals(1, m.getTotalErrors());
        assertTrue(m.getErrorsByModule().containsKey("BattleRoom"));
    }

    @Test
    @DisplayName("清空缓冲")
    void testClear() {
        logger.logLogin("p1", true, "test");
        assertEquals(1, logger.getRecentEntries().size());

        logger.clear();
        assertEquals(0, logger.getRecentEntries().size());
    }

    @Test
    @DisplayName("AuditLogEntry JSON 格式")
    void testEntryToJson() {
        AuditLogEntry entry = new AuditLogEntry(
            AuditLogType.LOGIN, "player-1", "room-001", "test detail", "LoginHandler");
        String json = entry.toJson();

        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("\"type\":\"LOGIN\""));
        assertTrue(json.contains("\"player\":\"player-1\""));
        assertTrue(json.contains("\"room\":\"room-001\""));
        assertTrue(json.contains("\"module\":\"LoginHandler\""));
    }
}
