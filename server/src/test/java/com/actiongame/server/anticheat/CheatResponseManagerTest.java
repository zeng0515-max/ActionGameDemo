package com.actiongame.server.anticheat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 作弊响应管理器单元测试
 */
class CheatResponseManagerTest {

    private CheatResponseManager manager;

    @BeforeEach
    void setUp() {
        manager = new CheatResponseManager();
    }

    @Test
    @DisplayName("低分违规: 无处罚")
    void testLowScore_NoPunish() {
        CheatIncident incident = new CheatIncident("p1", 1, CheatType.SPEED_HACK,
            CheatSeverity.LOW, System.currentTimeMillis(), "test");
        CheatResponseManager.PunishAction action = manager.submitIncident(incident);
        // LOW=10分, 达到WARN_THRESHOLD(10) → WARN
        assertEquals(CheatResponseManager.PunishAction.WARN, action);
    }

    @Test
    @DisplayName("中等分数: 警告")
    void testMediumScore_Warn() {
        // LOW=10分, 累计2次=20 → WARN
        for (int i = 0; i < 2; i++) {
            CheatIncident incident = new CheatIncident("p1", 1, CheatType.SPEED_HACK,
                CheatSeverity.LOW, System.currentTimeMillis(), "test");
            manager.submitIncident(incident);
        }
        assertEquals(20, manager.getViolationScore("p1"));

        // 再加一个LOW=10 → total=30 → ROLLBACK
        CheatIncident incident3 = new CheatIncident("p1", 1, CheatType.SPEED_HACK,
            CheatSeverity.LOW, System.currentTimeMillis(), "test");
        CheatResponseManager.PunishAction action = manager.submitIncident(incident3);
        assertEquals(CheatResponseManager.PunishAction.ROLLBACK, action);
    }

    @Test
    @DisplayName("高分: 踢出房间")
    void testHighScore_Kick() {
        // CRITICAL=40分 → KICK
        CheatIncident incident = new CheatIncident("p1", 1, CheatType.DAMAGE_HACK,
            CheatSeverity.CRITICAL, System.currentTimeMillis(), "test");
        CheatResponseManager.PunishAction action = manager.submitIncident(incident);
        assertEquals(CheatResponseManager.PunishAction.KICK, action);
    }

    @Test
    @DisplayName("极高分: 封禁")
    void testCriticalScore_Ban() {
        // CRITICAL=40 + CRITICAL=40 = 80 → BAN
        for (int i = 0; i < 2; i++) {
            CheatIncident incident = new CheatIncident("p1", 1, CheatType.DAMAGE_HACK,
                CheatSeverity.CRITICAL, System.currentTimeMillis(), "test");
            manager.submitIncident(incident);
        }
        assertTrue(manager.isBanned("p1"));
    }

    @Test
    @DisplayName("已封禁玩家: 持续返回BAN")
    void testBannedPlayer_AlwaysBan() {
        for (int i = 0; i < 2; i++) {
            CheatIncident incident = new CheatIncident("p1", 1, CheatType.DAMAGE_HACK,
                CheatSeverity.CRITICAL, System.currentTimeMillis(), "test");
            manager.submitIncident(incident);
        }
        assertTrue(manager.isBanned("p1"));

        CheatIncident lowIncident = new CheatIncident("p1", 1, CheatType.SPEED_HACK,
            CheatSeverity.LOW, System.currentTimeMillis(), "test");
        assertEquals(CheatResponseManager.PunishAction.BAN, manager.submitIncident(lowIncident));
    }

    @Test
    @DisplayName("违规分数衰减")
    void testScoreDecay() {
        CheatIncident incident = new CheatIncident("p1", 1, CheatType.SPEED_HACK,
            CheatSeverity.MEDIUM, System.currentTimeMillis(), "test");
        manager.submitIncident(incident);
        assertEquals(20, manager.getViolationScore("p1"));

        // 衰减5次
        for (int i = 0; i < 5; i++) {
            manager.decayScores();
        }
        assertEquals(15, manager.getViolationScore("p1"));
    }

    @Test
    @DisplayName("清除玩家记录")
    void testClearPlayer() {
        CheatIncident incident = new CheatIncident("p1", 1, CheatType.SPEED_HACK,
            CheatSeverity.MEDIUM, System.currentTimeMillis(), "test");
        manager.submitIncident(incident);
        assertEquals(20, manager.getViolationScore("p1"));

        manager.clearPlayer("p1");
        assertEquals(0, manager.getViolationScore("p1"));
        assertFalse(manager.isBanned("p1"));
    }
}
