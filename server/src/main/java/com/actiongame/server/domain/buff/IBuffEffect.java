package com.actiongame.server.domain.buff;

/**
 * Buff效果接口 (服务端战斗系统调用)
 * 生命周期: onApply → onUpdate(deltaTime) → onRemove
 * 堆叠时调用 onStack(newStacks)
 */
public interface IBuffEffect {
    void onApply(Buff buff);

    void onUpdate(Buff buff, float deltaTime);

    void onRemove(Buff buff);

    void onStack(Buff buff, int newStacks);
}
