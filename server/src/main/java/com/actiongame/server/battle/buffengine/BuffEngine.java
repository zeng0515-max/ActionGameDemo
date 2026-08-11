package com.actiongame.server.battle.buffengine;

import com.actiongame.server.config.BuffConfig;
import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.buff.BuffStackingRule;
import com.actiongame.server.domain.buff.BuffType;
import com.actiongame.server.domain.character.Character;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Buff引擎 (对应Unity BuffSystem)
 * 管理角色身上所有Buff的添加/移除/Tick更新
 */
public class BuffEngine {
    private static final Logger log = LoggerFactory.getLogger(BuffEngine.class);

    private final Character owner;
    private final CopyOnWriteArrayList<ActiveBuff> activeBuffs = new CopyOnWriteArrayList<>();

    private static class ActiveBuff {
        final Buff buff;
        final IBuffEffectHandler handler;
        final Character source;

        ActiveBuff(Buff buff, IBuffEffectHandler handler, Character source) {
            this.buff = buff;
            this.handler = handler;
            this.source = source;
        }
    }

    public BuffEngine(Character owner) {
        this.owner = owner;
    }

    public Buff addBuff(BuffConfig config, Character source) {
        ActiveBuff existing = findActiveBuffByName(config.getBuffName());

        if (existing != null) {
            BuffStackingRule rule = config.getStackingRule();
            switch (rule) {
                case REFRESH_DURATION -> {
                    existing.buff.refreshDuration();
                    log.debug("Buff refreshed: {} on {}", config.getBuffName(), owner.getEntityId());
                    return existing.buff;
                }
                case STACK_STACKS -> {
                    int newStacks = Math.min(existing.buff.getStacks() + 1, existing.buff.getMaxStacks());
                    existing.buff.setStacks(newStacks);
                    existing.buff.refreshDuration();
                    existing.handler.onStack(existing.buff, owner, existing.source, newStacks);
                    log.debug("Buff stacked: {} x{} on {}", config.getBuffName(), newStacks, owner.getEntityId());
                    return existing.buff;
                }
                case TAKE_HIGHEST -> {
                    existing.buff.refreshDuration();
                    return existing.buff;
                }
            }
        }

        Buff buff = BuffFactory.createBuff(config);
        buff.setTargetEntityId(owner.getEntityId());
        if (source != null) buff.setSourceEntityId(source.getEntityId());
        buff.setActive(true);

        IBuffEffectHandler handler = BuffFactory.createEffectHandler(config.getBuffType());
        handler.onApply(buff, owner, source);

        activeBuffs.add(new ActiveBuff(buff, handler, source));
        log.debug("Buff added: {} on {}", config.getBuffName(), owner.getEntityId());
        return buff;
    }

    public void restoreBuff(Buff buff, Character source) {
        IBuffEffectHandler handler = BuffFactory.createEffectHandler(buff.getBuffType(), buff.getBuffName());
        handler.onApply(buff, owner, source);
        activeBuffs.add(new ActiveBuff(buff, handler, source));
        log.info("Buff restored: {} on {}", buff.getBuffName(), owner.getEntityId());
    }

    public void removeBuff(Buff buff) {
        for (ActiveBuff ab : activeBuffs) {
            if (ab.buff == buff) {
                ab.handler.onRemove(ab.buff, owner, ab.source);
                ab.buff.setActive(false);
                activeBuffs.remove(ab);
                return;
            }
        }
    }

    public void removeBuffByName(String buffName) {
        for (ActiveBuff ab : activeBuffs) {
            if (ab.buff.getBuffName().equals(buffName)) {
                ab.handler.onRemove(ab.buff, owner, ab.source);
                ab.buff.setActive(false);
                activeBuffs.remove(ab);
            }
        }
    }

    public void removeBuffByType(BuffType type) {
        for (ActiveBuff ab : activeBuffs) {
            if (ab.buff.getBuffType() == type) {
                ab.handler.onRemove(ab.buff, owner, ab.source);
                ab.buff.setActive(false);
                activeBuffs.remove(ab);
            }
        }
    }

    public void clearAllBuffs() {
        for (ActiveBuff ab : activeBuffs) {
            ab.handler.onRemove(ab.buff, owner, ab.source);
            ab.buff.setActive(false);
        }
        activeBuffs.clear();
        owner.getStats().resetAllModifiers();
    }

    public void update(float deltaTime) {
        List<ActiveBuff> expired = null;

        for (ActiveBuff ab : activeBuffs) {
            ab.buff.update(deltaTime);
            ab.handler.onUpdate(ab.buff, owner, ab.source, deltaTime);

            if (ab.buff.isExpired()) {
                if (expired == null) expired = new java.util.ArrayList<>();
                expired.add(ab);
            }
        }

        // 在迭代结束后统一移除过期 buff, 避免在 CopyOnWriteArrayList 遍历中 remove 导致额外快照拷贝
        if (expired != null) {
            for (ActiveBuff ab : expired) {
                ab.handler.onRemove(ab.buff, owner, ab.source);
                ab.buff.setActive(false);
                activeBuffs.remove(ab);
                log.debug("Buff expired: {} from {}", ab.buff.getBuffName(), owner.getEntityId());
            }
        }
    }

    public int getBuffCount() { return activeBuffs.size(); }

    public List<Buff> getActiveBuffs() {
        return activeBuffs.stream().map(ab -> ab.buff).toList();
    }

    public ActiveBuff findActiveBuffByName(String buffName) {
        for (ActiveBuff ab : activeBuffs) {
            if (ab.buff.getBuffName().equals(buffName)) return ab;
        }
        return null;
    }

    public Buff findBuffByName(String buffName) {
        ActiveBuff ab = findActiveBuffByName(buffName);
        return ab != null ? ab.buff : null;
    }

    public boolean hasBuff(String buffName) {
        return findActiveBuffByName(buffName) != null;
    }
}
