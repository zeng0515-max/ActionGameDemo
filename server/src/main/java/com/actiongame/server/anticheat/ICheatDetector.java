package com.actiongame.server.anticheat;

import com.actiongame.server.domain.character.Character;
import com.actiongame.server.replay.BattleRecording;

/**
 * 反作弊检测接口 (Phase 8 三层反作弊预留)
 *
 * 三层架构:
 * 1. 客户端行为分析 (移动速度/攻击频率异常)
 * 2. 服务端逻辑校验 (伤害/属性范围校验)
 * 3. 回放统计分析 (离线异常检测)
 */
public interface ICheatDetector {

    /**
     * 第一层: 实时行为检测 (每帧调用)
     * @param maxMoveSpeed 角色实际最大移速 (从 CharacterStats 读取)
     */
    boolean checkRealtime(int entityId, int actionType, float moveX, float moveZ, long timestamp, float maxMoveSpeed);

    /**
     * 第二层: 逻辑校验 (伤害结算时调用)
     */
    boolean checkLogic(Character attacker, Character target, float damage);

    /**
     * 第三层: 离线回放分析
     */
    int analyzeReplay(BattleRecording recording);
}
