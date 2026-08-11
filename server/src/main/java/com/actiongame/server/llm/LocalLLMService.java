package com.actiongame.server.llm;

import com.actiongame.server.anticheat.CheatIncident;
import com.actiongame.server.anticheat.CheatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 本地规则引擎 LLM 实现 — 无外部 API 依赖, 离线可运行
 *
 * 使用规则模板 + 随机变体生成解说文本
 * 难度分析基于玩家表现指标
 * 作弊分析基于事件模式匹配
 *
 * 未来可替换为真实 LLM API (OpenAI/火山引擎/通义千问)
 */
public class LocalLLMService implements LLMService {
    private static final Logger log = LoggerFactory.getLogger(LocalLLMService.class);

    // === 战斗解说模板 ===
    private static final String[] BATTLE_START_TEMPLATES = {
        "战斗开始! %d名玩家面对%d只怪物的挑战",
        "房间%s的战斗已打响, %d只怪物蓄势待发",
        "勇者们进入战场, %d只怪物在前方等待"
    };

    private static final String[] HIGH_COMBO_TEMPLATES = {
        "精彩的%d连击! 玩家展现出惊人的操作!",
        "连击数达到%d! 怪物无力招架!",
        "%d连击! 这一波操作堪称完美!"
    };

    private static final String[] LOW_HP_TEMPLATES = {
        "警告: 玩家生命值仅剩%.0f%%, 情况危急!",
        "玩家血量告急 (%.0f%%), 需要谨慎应对",
        "危险! 玩家HP降至%.0f%%, 一击即溃!"
    };

    private static final String[] MONSTER_KILLED_TEMPLATES = {
        "怪物被击败! 还剩%d只",
        "又一只怪物倒下, 剩余%d只",
        "干净利落的击杀! 剩余怪物%d只"
    };

    private static final String[] VICTORY_TEMPLATES = {
        "胜利! 玩家在%.1f秒内消灭了所有怪物",
        "战斗结束! 总伤害%.0f, 最高连击%d, 完美的战斗!",
        "恭喜通关! 这场战斗持续了%.1f秒, 表现优异"
    };

    private static final String[] DEFEAT_TEMPLATES = {
        "玩家被击败... 怪物还剩%.0f%%血量",
        "战斗失败, 再接再厉!",
        "不幸落败, 怪物剩余血量%.0f%%"
    };

    // === 接口实现 ===

    @Override
    public String generateBattleNarration(BattleContext context) {
        if (context == null) return "";

        // 根据上下文选择最合适的解说
        if (context.getMaxCombo() >= 4) {
            return pick(HIGH_COMBO_TEMPLATES, context.getMaxCombo());
        }

        if (context.getAvgPlayerHpPercent() > 0 && context.getAvgPlayerHpPercent() < 0.3f) {
            return String.format(pick(LOW_HP_TEMPLATES), context.getAvgPlayerHpPercent() * 100);
        }

        if (context.getMonstersKilled() > 0 && context.getMonsterCount() > 0) {
            int remaining = context.getMonsterCount() - context.getMonstersKilled();
            return String.format(pick(MONSTER_KILLED_TEMPLATES), remaining);
        }

        if (context.getTotalFrames() < 50) {
            return String.format(pick(BATTLE_START_TEMPLATES),
                context.getPlayerCount(), context.getMonsterCount());
        }

        return String.format("战斗进行中: 第%d帧, 玩家HP=%.0f%%",
            context.getTotalFrames(), context.getAvgPlayerHpPercent() * 100);
    }

    @Override
    public DifficultySuggestion analyzeDifficulty(BattleContext context) {
        if (context == null) return DifficultySuggestion.noChange("No context");

        float playerHp = context.getAvgPlayerHpPercent();
        float hitRate = context.getHitRate();
        float dps = context.getDPS();

        // 玩家表现过好 (高HP + 高命中率 + 高DPS)
        if (playerHp > 0.8f && hitRate > 0.8f && dps > 50f) {
            float pct = 0.15f + (playerHp - 0.8f) * 0.5f; // 15%-25%
            return new DifficultySuggestion(
                DifficultySuggestion.Action.INCREASE_HEALTH, pct,
                String.format("玩家表现优异 (HP=%.0f%%, 命中率=%.0f%%, DPS=%.0f), 建议提升怪物血量%.0f%%",
                    playerHp * 100, hitRate * 100, dps, pct * 100));
        }

        // 玩家表现差 (低HP + 低命中率)
        if (playerHp < 0.3f && hitRate < 0.4f) {
            float pct = 0.2f + (0.3f - playerHp) * 0.5f; // 20%-35%
            return new DifficultySuggestion(
                DifficultySuggestion.Action.DECREASE_HEALTH, pct,
                String.format("玩家陷入困境 (HP=%.0f%%, 命中率=%.0f%%), 建议降低怪物血量%.0f%%",
                    playerHp * 100, hitRate * 100, pct * 100));
        }

        // DPS 过低, 玩家攻击力不足
        if (dps > 0 && dps < 5f && context.getDurationSeconds() > 10f) {
            return new DifficultySuggestion(
                DifficultySuggestion.Action.DECREASE_DAMAGE, 0.1f,
                String.format("玩家DPS=%.1f过低, 建议降低怪物攻击力10%%", dps));
        }

        // DPS 过高, 怪物被秒杀
        if (dps > 200f && context.getDurationSeconds() < 5f) {
            return new DifficultySuggestion(
                DifficultySuggestion.Action.SPAWN_MORE, 0f,
                String.format("玩家DPS=%.1f过高, 战斗仅%.1f秒, 建议增加怪物数量", dps, context.getDurationSeconds()));
        }

        return DifficultySuggestion.noChange(
            String.format("难度适中 (HP=%.0f%%, 命中率=%.0f%%, DPS=%.0f)",
                playerHp * 100, hitRate * 100, dps));
    }

    @Override
    public CheatAnalysis analyzeCheatPatterns(BattleContext context) {
        if (context == null || context.getCheatIncidents() == null || context.getCheatIncidents().isEmpty()) {
            return CheatAnalysis.noRisk();
        }

        List<CheatIncident> incidents = context.getCheatIncidents();
        List<String> patterns = new ArrayList<>();
        int score = 0;

        // 统计作弊类型分布
        Map<CheatType, Integer> typeCounts = new HashMap<>();
        for (CheatIncident inc : incidents) {
            typeCounts.merge(inc.getCheatType(), 1, Integer::sum);
        }

        // 模式1: 重复同类型作弊 (单一类型 > 3次)
        for (var entry : typeCounts.entrySet()) {
            if (entry.getValue() >= 3) {
                patterns.add(String.format("重复%s: %d次 (%s)",
                    entry.getKey().getDescription(), entry.getValue(), entry.getKey()));
                score += entry.getValue() * 10;
            }
        }

        // 模式2: 多种作弊类型同时出现
        if (typeCounts.size() >= 3) {
            patterns.add("多种作弊类型同时出现: " + typeCounts.size() + "种");
            score += 20;
        }

        // 模式3: DPS 异常
        float dps = context.getDPS();
        if (dps > 5000f) {
            patterns.add(String.format("异常DPS: %.1f (阈值5000)", dps));
            score += 25;
        }

        // 模式4: 命中率异常
        float hitRate = context.getHitRate();
        if (hitRate > 0.99f && context.getTotalAttacks() > 20) {
            patterns.add(String.format("命中率异常: %.2f%% (20+次攻击)", hitRate * 100));
            score += 15;
        }

        // 模式5: 攻击频率异常
        if (context.getTotalAttacks() > 0) {
            float attacksPerSec = context.getTotalAttacks() / context.getDurationSeconds();
            if (attacksPerSec > 10f) {
                patterns.add(String.format("攻击频率异常: %.1f次/秒", attacksPerSec));
                score += 20;
            }
        }

        // 确定风险等级
        CheatAnalysis.RiskLevel riskLevel;
        String recommendation;
        if (score >= 80) {
            riskLevel = CheatAnalysis.RiskLevel.CRITICAL;
            recommendation = "建议立即封禁该玩家";
        } else if (score >= 50) {
            riskLevel = CheatAnalysis.RiskLevel.HIGH;
            recommendation = "建议踢出房间并标记";
        } else if (score >= 25) {
            riskLevel = CheatAnalysis.RiskLevel.MEDIUM;
            recommendation = "建议警告并密切监控";
        } else if (score > 0) {
            riskLevel = CheatAnalysis.RiskLevel.LOW;
            recommendation = "记录异常, 可能是误判";
        } else {
            riskLevel = CheatAnalysis.RiskLevel.NONE;
            recommendation = "无需处理";
        }

        score = Math.min(100, score);

        String summary = String.format("检测到%d个作弊事件, %d种类型, 风险评分=%d",
            incidents.size(), typeCounts.size(), score);

        log.info("[LLM] Cheat analysis: risk={}, score={}, patterns={}", riskLevel, score, patterns.size());

        return new CheatAnalysis(riskLevel, score, summary, patterns, recommendation);
    }

    @Override
    public String generateBattleSummary(BattleContext context) {
        if (context == null) return "战斗结束";

        if (context.getBattleResult() == 1) {
            // 胜利 - 随机选模板, 确保参数类型匹配
            int idx = ThreadLocalRandom.current().nextInt(VICTORY_TEMPLATES.length);
            String template = VICTORY_TEMPLATES[idx];
            if (idx == 1) {
                return String.format(template, context.getTotalDamage(), context.getMaxCombo());
            } else {
                return String.format(template, context.getDurationSeconds());
            }
        } else if (context.getBattleResult() == 2) {
            // 失败
            int idx = ThreadLocalRandom.current().nextInt(DEFEAT_TEMPLATES.length);
            String template = DEFEAT_TEMPLATES[idx];
            if (idx == 0 || idx == 2) {
                return String.format(template, context.getAvgMonsterHpPercent() * 100);
            } else {
                return template;
            }
        }

        // 未知结果
        return String.format("战斗结束: %d帧, %.1f秒, 总伤害%.0f, 最高连击%d",
            context.getTotalFrames(), context.getDurationSeconds(),
            context.getTotalDamage(), context.getMaxCombo());
    }

    // === 辅助方法 ===

    private static String pick(String[] templates, Object... args) {
        String template = templates[ThreadLocalRandom.current().nextInt(templates.length)];
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }
}
