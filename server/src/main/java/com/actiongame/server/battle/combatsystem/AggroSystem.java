package com.actiongame.server.battle.combatsystem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仇恨系统 (对应文档3.3.4 AggroSystem)
 * 管理每个角色对其他角色的仇恨值
 */
public class AggroSystem {
    private final int ownerId;
    private final ConcurrentHashMap<Integer, Float> aggroTable = new ConcurrentHashMap<>();

    public AggroSystem(int ownerId) {
        this.ownerId = ownerId;
    }

    public void addAggro(int targetEntityId, float amount) {
        aggroTable.merge(targetEntityId, amount, Float::sum);
    }

    public void setAggro(int targetEntityId, float amount) {
        aggroTable.put(targetEntityId, amount);
    }

    public int getTopAggroTarget() {
        int topTarget = -1;
        float topAggro = 0f;
        for (Map.Entry<Integer, Float> entry : aggroTable.entrySet()) {
            if (entry.getValue() > topAggro) {
                topAggro = entry.getValue();
                topTarget = entry.getKey();
            }
        }
        return topTarget;
    }

    public float getAggro(int targetEntityId) {
        return aggroTable.getOrDefault(targetEntityId, 0f);
    }

    public void decayAll(float factor) {
        aggroTable.replaceAll((k, v) -> v * factor);
        aggroTable.entrySet().removeIf(e -> e.getValue() < 1f);
    }

    public void clear() {
        aggroTable.clear();
    }

    public void removeTarget(int targetEntityId) {
        aggroTable.remove(targetEntityId);
    }

    public int getOwnerId() { return ownerId; }
    public boolean hasTarget() { return !aggroTable.isEmpty(); }
    public Map<Integer, Float> getAggroTable() { return aggroTable; }
}
