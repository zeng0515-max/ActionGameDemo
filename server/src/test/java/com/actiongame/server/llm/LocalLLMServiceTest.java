package com.actiongame.server.llm;

import com.actiongame.server.anticheat.CheatIncident;
import com.actiongame.server.anticheat.CheatSeverity;
import com.actiongame.server.anticheat.CheatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LLM 本地规则引擎单元测试
 */
class LocalLLMServiceTest {

    private LocalLLMService llm;

    @BeforeEach
    void setUp() {
        llm = new LocalLLMService();
    }

    // === 战斗解说 ===

    @Test
    @DisplayName("战斗开始解说")
    void testBattleStartNarration() {
        var ctx = new BattleContext("room-001", 10, 200);
        ctx.setPlayerCount(1);
        ctx.setMonsterCount(4);

        String narration = llm.generateBattleNarration(ctx);
        assertNotNull(narration);
        assertFalse(narration.isEmpty());
        // 开局模板包含玩家数或怪物数
        assertTrue(narration.contains("1") || narration.contains("4") || narration.contains("战斗"));
    }

    @Test
    @DisplayName("高连击解说")
    void testHighComboNarration() {
        var ctx = new BattleContext("room-001", 100, 2000);
        ctx.setMaxCombo(5);

        String narration = llm.generateBattleNarration(ctx);
        assertTrue(narration.contains("5"), "Should mention combo count");
    }

    @Test
    @DisplayName("低血量解说")
    void testLowHpNarration() {
        var ctx = new BattleContext("room-001", 200, 4000);
        ctx.setMaxCombo(0);
        ctx.setAvgPlayerHpPercent(0.2f);

        String narration = llm.generateBattleNarration(ctx);
        assertTrue(narration.contains("20"), "Should mention HP percent");
    }

    @Test
    @DisplayName("null 上下文返回空字符串")
    void testNullContextNarration() {
        assertEquals("", llm.generateBattleNarration(null));
    }

    // === 难度分析 ===

    @Test
    @DisplayName("玩家表现优异: 建议提升怪物血量")
    void testHighPerformanceDifficulty() {
        var ctx = new BattleContext("room-001", 500, 10000);
        ctx.setAvgPlayerHpPercent(0.95f);
        ctx.setTotalAttacks(20);
        ctx.setTotalHits(18);
        ctx.setTotalDamage(600f); // 60 DPS > 50

        var suggestion = llm.analyzeDifficulty(ctx);
        assertEquals(DifficultySuggestion.Action.INCREASE_HEALTH, suggestion.getAction());
        assertTrue(suggestion.getAdjustmentPercent() > 0);
    }

    @Test
    @DisplayName("玩家表现差: 建议降低怪物血量")
    void testLowPerformanceDifficulty() {
        var ctx = new BattleContext("room-001", 500, 10000);
        ctx.setAvgPlayerHpPercent(0.15f);
        ctx.setTotalAttacks(20);
        ctx.setTotalHits(5);
        ctx.setTotalDamage(50f);

        var suggestion = llm.analyzeDifficulty(ctx);
        assertEquals(DifficultySuggestion.Action.DECREASE_HEALTH, suggestion.getAction());
        assertTrue(suggestion.getAdjustmentPercent() > 0);
    }

    @Test
    @DisplayName("难度适中: 无需调整")
    void testBalancedDifficulty() {
        var ctx = new BattleContext("room-001", 500, 10000);
        ctx.setAvgPlayerHpPercent(0.6f);
        ctx.setTotalAttacks(20);
        ctx.setTotalHits(12);
        ctx.setTotalDamage(100f);

        var suggestion = llm.analyzeDifficulty(ctx);
        assertEquals(DifficultySuggestion.Action.NO_CHANGE, suggestion.getAction());
    }

    @Test
    @DisplayName("DPS过低: 建议降低怪物攻击力")
    void testLowDPSDifficulty() {
        var ctx = new BattleContext("room-001", 1000, 15000);
        ctx.setAvgPlayerHpPercent(0.5f);
        ctx.setTotalAttacks(15);
        ctx.setTotalHits(10);
        ctx.setTotalDamage(50f); // 15s / 50 = 3.3 DPS

        var suggestion = llm.analyzeDifficulty(ctx);
        assertEquals(DifficultySuggestion.Action.DECREASE_DAMAGE, suggestion.getAction());
    }

    @Test
    @DisplayName("DPS过高: 建议增加怪物数量")
    void testHighDPSDifficulty() {
        var ctx = new BattleContext("room-001", 100, 3000); // 3秒
        ctx.setAvgPlayerHpPercent(0.5f);
        ctx.setTotalAttacks(10);
        ctx.setTotalHits(8);
        ctx.setTotalDamage(1000f); // 1000/3 = 333 DPS

        var suggestion = llm.analyzeDifficulty(ctx);
        assertEquals(DifficultySuggestion.Action.SPAWN_MORE, suggestion.getAction());
    }

    // === 作弊分析 ===

    @Test
    @DisplayName("无作弊事件: 无风险")
    void testNoCheatRisk() {
        var ctx = new BattleContext("room-001", 500, 10000);
        var analysis = llm.analyzeCheatPatterns(ctx);
        assertEquals(CheatAnalysis.RiskLevel.NONE, analysis.getRiskLevel());
        assertEquals(0, analysis.getRiskScore());
    }

    @Test
    @DisplayName("少量作弊: 低风险")
    void testLowCheatRisk() {
        var ctx = new BattleContext("room-001", 500, 10000);
        ctx.setCheatIncidents(List.of(
            new CheatIncident("p1", 1, CheatType.SPEED_HACK, CheatSeverity.LOW,
                System.currentTimeMillis(), "test")
        ));

        var analysis = llm.analyzeCheatPatterns(ctx);
        assertTrue(analysis.getRiskScore() <= 25);
        assertTrue(analysis.getRiskLevel() == CheatAnalysis.RiskLevel.LOW ||
                  analysis.getRiskLevel() == CheatAnalysis.RiskLevel.NONE);
    }

    @Test
    @DisplayName("重复作弊: 中高风险")
    void testRepeatedCheatRisk() {
        var ctx = new BattleContext("room-001", 500, 10000);
        List<CheatIncident> incidents = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            incidents.add(new CheatIncident("p1", 1, CheatType.SPEED_HACK,
                CheatSeverity.HIGH, System.currentTimeMillis(), "speed=" + (20 + i)));
        }
        ctx.setCheatIncidents(incidents);

        var analysis = llm.analyzeCheatPatterns(ctx);
        assertTrue(analysis.getRiskScore() >= 25);
        assertTrue(analysis.getPatterns().size() > 0);
    }

    @Test
    @DisplayName("多种作弊类型: 中高风险")
    void testMultipleCheatTypesRisk() {
        var ctx = new BattleContext("room-001", 500, 10000);
        List<CheatIncident> incidents = new ArrayList<>();
        // 每种类型 3 次 (触发重复检测) × 3 种类型 = 9 个事件
        for (int i = 0; i < 3; i++) {
            incidents.add(new CheatIncident("p1", 1, CheatType.SPEED_HACK,
                CheatSeverity.MEDIUM, 0, "v" + i));
            incidents.add(new CheatIncident("p1", 1, CheatType.DAMAGE_HACK,
                CheatSeverity.HIGH, 0, "v" + i));
            incidents.add(new CheatIncident("p1", 1, CheatType.ATTACK_RATE_HACK,
                CheatSeverity.MEDIUM, 0, "v" + i));
        }
        ctx.setCheatIncidents(incidents);

        var analysis = llm.analyzeCheatPatterns(ctx);
        assertTrue(analysis.getRiskScore() >= 50, "Score should be >= 50, got " + analysis.getRiskScore());
        assertTrue(analysis.getPatterns().size() >= 2);
    }

    @Test
    @DisplayName("异常DPS检测")
    void testAbnormalDPSDetection() {
        var ctx = new BattleContext("room-001", 500, 1000); // 1秒
        ctx.setCheatIncidents(List.of(
            new CheatIncident("p1", 1, CheatType.DAMAGE_HACK, CheatSeverity.CRITICAL, 0, "test")
        ));
        ctx.setTotalDamage(10000f); // 10000 DPS

        var analysis = llm.analyzeCheatPatterns(ctx);
        assertTrue(analysis.getRiskScore() >= 25);
        assertTrue(analysis.getPatterns().stream().anyMatch(p -> p.contains("DPS")));
    }

    // === 战斗总结 ===

    @Test
    @DisplayName("胜利总结")
    void testVictorySummary() {
        var ctx = new BattleContext("room-001", 500, 10000);
        ctx.setBattleResult(1);
        ctx.setTotalDamage(500f);
        ctx.setMaxCombo(4);

        String summary = llm.generateBattleSummary(ctx);
        assertNotNull(summary);
        assertFalse(summary.isEmpty());
    }

    @Test
    @DisplayName("失败总结")
    void testDefeatSummary() {
        var ctx = new BattleContext("room-001", 500, 10000);
        ctx.setBattleResult(2);
        ctx.setAvgMonsterHpPercent(0.5f);

        String summary = llm.generateBattleSummary(ctx);
        assertNotNull(summary);
        assertFalse(summary.isEmpty());
    }

    @Test
    @DisplayName("null 上下文总结")
    void testNullSummary() {
        assertEquals("战斗结束", llm.generateBattleSummary(null));
    }

    // === BattleContext 辅助方法 ===

    @Test
    @DisplayName("命中率计算")
    void testHitRateCalculation() {
        var ctx = new BattleContext("room-1", 100, 2000);
        ctx.setTotalAttacks(20);
        ctx.setTotalHits(15);
        assertEquals(0.75f, ctx.getHitRate(), 0.001f);
    }

    @Test
    @DisplayName("DPS计算")
    void testDPSCalculation() {
        var ctx = new BattleContext("room-1", 100, 5000); // 5秒
        ctx.setTotalDamage(250f);
        assertEquals(50f, ctx.getDPS(), 0.1f);
    }
}
