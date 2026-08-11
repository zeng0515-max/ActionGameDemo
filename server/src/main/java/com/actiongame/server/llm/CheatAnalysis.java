package com.actiongame.server.llm;

import com.actiongame.server.anticheat.CheatIncident;
import com.actiongame.server.anticheat.CheatType;

import java.util.List;

/**
 * 作弊模式分析报告
 */
public class CheatAnalysis {

    public enum RiskLevel {
        NONE,       // 无风险
        LOW,        // 低风险: 个别异常, 可能误判
        MEDIUM,     // 中风险: 多个异常, 疑似作弊
        HIGH,       // 高风险: 明确作弊模式
        CRITICAL    // 极高风险: 恶意作弊, 建议封禁
    }

    private final RiskLevel riskLevel;
    private final int riskScore;        // 0-100
    private final String summary;       // 分析摘要
    private final List<String> patterns; // 识别到的作弊模式
    private final String recommendation; // 处理建议

    public CheatAnalysis(RiskLevel riskLevel, int riskScore, String summary,
                         List<String> patterns, String recommendation) {
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.summary = summary;
        this.patterns = patterns;
        this.recommendation = recommendation;
    }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public int getRiskScore() { return riskScore; }
    public String getSummary() { return summary; }
    public List<String> getPatterns() { return patterns; }
    public String getRecommendation() { return recommendation; }

    @Override
    public String toString() {
        return String.format("CheatAnalysis{risk=%s(score=%d), patterns=%s, rec=%s}",
            riskLevel, riskScore, patterns, recommendation);
    }

    public static CheatAnalysis noRisk() {
        return new CheatAnalysis(RiskLevel.NONE, 0, "No anomalies detected",
            List.of(), "No action needed");
    }
}
