package com.actiongame.server.llm;

/**
 * LLM 服务接口 — 战斗解说、难度分析、作弊模式识别
 *
 * 设计原则:
 * 1. 接口与实现解耦: 本地规则引擎可离线运行, 未来可替换为真实 LLM API
 * 2. 异步调用: 所有方法返回非阻塞, 结果通过回调消费
 * 3. 降级策略: LLM 不可用时返回默认文案, 不影响战斗流程
 */
public interface LLMService {

    /**
     * 生成战斗解说文本
     * @param context 战斗上下文 (房间ID, 帧数, 玩家/怪物状态)
     * @return 解说文本 (如"玩家发动了凌厉的连击, 怪物节节败退!")
     */
    String generateBattleNarration(BattleContext context);

    /**
     * 分析玩家表现并给出难度调整建议
     * @param context 战斗上下文
     * @return 难度建议 (如"玩家表现优异, 建议提升怪物攻击力20%")
     */
    DifficultySuggestion analyzeDifficulty(BattleContext context);

    /**
     * 分析作弊模式 (基于回放数据)
     * @param context 战斗上下文 (含作弊事件列表)
     * @return 作弊分析报告
     */
    CheatAnalysis analyzeCheatPatterns(BattleContext context);

    /**
     * 生成战斗结束总结
     * @param context 战斗上下文
     * @return 战斗总结文本
     */
    String generateBattleSummary(BattleContext context);
}
