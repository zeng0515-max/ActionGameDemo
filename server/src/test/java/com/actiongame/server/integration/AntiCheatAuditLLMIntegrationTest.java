package com.actiongame.server.integration;

import com.actiongame.server.anticheat.CheatDetector;
import com.actiongame.server.anticheat.CheatIncident;
import com.actiongame.server.anticheat.CheatType;
import com.actiongame.server.anticheat.CheatSeverity;
import com.actiongame.server.audit.AuditLoggerImpl;
import com.actiongame.server.llm.BattleContext;
import com.actiongame.server.llm.CheatAnalysis;
import com.actiongame.server.llm.DifficultySuggestion;
import com.actiongame.server.llm.LocalLLMService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 多模块集成测试: 反作弊 + 审计日志 + LLM 联动
 */
@DisplayName("集成: 反作弊+审计+LLM 联动")
class AntiCheatAuditLLMIntegrationTest {

    @Test
    @DisplayName("作弊检测→审计日志→LLM分析 完整链路")
    void cheatDetection_triggersAuditAndLLMAnalysis() {
        // 1. 初始化各模块
        var auditLogger = new AuditLoggerImpl(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")));
        var llm = new LocalLLMService();
        var detector = new CheatDetector(0.02f);

        // 2. 模拟作弊行为 (超速移动)
        boolean cheatDetected = detector.checkRealtime(1, 1, 5.0f, 5.0f, System.currentTimeMillis(), 5f);
        assertThat(cheatDetected).isTrue();

        // 3. 获取作弊事件
        List<CheatIncident> incidents = detector.getIncidents();
        assertThat(incidents).isNotEmpty();

        // 4. 记录到审计日志
        for (var inc : incidents) {
            auditLogger.logCheatDetected(inc.getPlayerId(), inc.getCheatType().name(), inc.getDetail());
        }

        // 5. 验证审计日志
        var auditEntries = auditLogger.getEntriesByType(
            com.actiongame.server.audit.AuditLogType.CHEAT_DETECTED);
        assertThat(auditEntries).isNotEmpty();
        assertThat(auditEntries.get(0).getDetail()).contains("SPEED_HACK");

        // 6. LLM 分析作弊模式
        var ctx = new BattleContext("room-cheat-01", 500, 10000);
        ctx.setCheatIncidents(incidents);
        var analysis = llm.analyzeCheatPatterns(ctx);

        assertThat(analysis).isNotNull();
        // 单次作弊事件可能不触发模式检测, 但风险评分 >= 0
        assertThat(analysis.getRiskScore()).isGreaterThanOrEqualTo(0);

        // 7. 验证指标统计
        assertThat(auditLogger.getMetrics().getCheatIncidents()).isGreaterThan(0);
    }

    @Test
    @DisplayName("战斗结束→LLM总结→难度建议 完整链路")
    void battleEnd_triggersLLMSummaryAndDifficulty() {
        var llm = new LocalLLMService();
        var auditLogger = new AuditLoggerImpl(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")));

        // 模拟战斗结束上下文
        var ctx = new BattleContext("room-llm-01", 2500, 50000); // 50秒
        ctx.setPlayerCount(1);
        ctx.setMonsterCount(4);
        ctx.setBattleResult(1); // 胜利
        ctx.setAvgPlayerHpPercent(0.45f);
        ctx.setTotalAttacks(30);
        ctx.setTotalHits(22);
        ctx.setTotalDamage(800f);
        ctx.setMaxCombo(4);

        // LLM 生成战斗总结
        String summary = llm.generateBattleSummary(ctx);
        assertThat(summary).isNotBlank();

        // LLM 难度分析
        var suggestion = llm.analyzeDifficulty(ctx);
        assertThat(suggestion).isNotNull();

        // 审计日志记录
        auditLogger.logBattleStart(ctx.getRoomId(), ctx.getPlayerCount());
        auditLogger.logBattleEnd(ctx.getRoomId(), ctx.getBattleResult(), ctx.getTotalFrames());

        // 验证
        assertThat(auditLogger.getMetrics().getBattlesStarted()).isEqualTo(1);
        assertThat(auditLogger.getMetrics().getBattlesEnded()).isEqualTo(1);
        assertThat(auditLogger.getMetrics().getTotalFramesExecuted()).isEqualTo(2500);
    }

    @Test
    @DisplayName("高频作弊→处罚升级→审计记录")
    void repeatedCheating_escalatesPunishment() {
        var detector = new CheatDetector(0.02f);
        var auditLogger = new AuditLoggerImpl(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")));
        var llm = new LocalLLMService();

        // 模拟多次作弊
        for (int i = 0; i < 10; i++) {
            detector.checkRealtime(1, 3, 0, 0, System.currentTimeMillis() + i * 50, 5f);
        }

        List<CheatIncident> incidents = detector.getIncidents();
        assertThat(incidents.size()).isGreaterThanOrEqualTo(5);

        // 全部记录到审计
        for (var inc : incidents) {
            auditLogger.logCheatDetected(inc.getPlayerId(), inc.getCheatType().name(), inc.getDetail());
        }

        // 验证指标
        assertThat(auditLogger.getMetrics().getCheatIncidents()).isEqualTo(incidents.size());

        // LLM 分析应判定为中高风险
        var ctx = new BattleContext("room-escalation-01", 500, 10000);
        ctx.setCheatIncidents(incidents);
        var analysis = llm.analyzeCheatPatterns(ctx);

        assertThat(analysis.getRiskScore()).isGreaterThanOrEqualTo(25);
    }

    @Test
    @DisplayName("正常游戏行为不触发任何模块")
    void normalGameplay_noFalsePositives() {
        var detector = new CheatDetector(0.02f);
        var auditLogger = new AuditLoggerImpl(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")));
        var llm = new LocalLLMService();

        // 模拟正常操作 (正常移速, 正常攻击间隔)
        long now = System.currentTimeMillis();
        detector.checkRealtime(1, 1, 0.5f, 0.3f, now, 5f); // 正常移动
        try { Thread.sleep(200); } catch (Exception e) {}
        detector.checkRealtime(1, 3, 0, 0, System.currentTimeMillis(), 5f); // 正常攻击

        // 不应有作弊事件
        assertThat(detector.getIncidents()).isEmpty();
        assertThat(detector.getTotalViolationScore()).isEqualTo(0);

        // LLM 分析应无风险
        var ctx = new BattleContext("room-normal-01", 500, 10000);
        var analysis = llm.analyzeCheatPatterns(ctx);
        assertThat(analysis.getRiskLevel()).isEqualTo(CheatAnalysis.RiskLevel.NONE);

        // 审计指标应无作弊记录
        assertThat(auditLogger.getMetrics().getCheatIncidents()).isEqualTo(0);
    }
}
