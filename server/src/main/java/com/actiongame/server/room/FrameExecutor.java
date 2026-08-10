package com.actiongame.server.room;

/**
 * 帧执行器接口
 * 帧内执行顺序: AI → 移动 → 战斗 → Buff → 快照
 */
public interface FrameExecutor {
    /**
     * 执行一帧
     * @param frameIndex 帧索引
     * @param deltaTime 时间增量 (秒)
     */
    void executeFrame(long frameIndex, float deltaTime);
}
