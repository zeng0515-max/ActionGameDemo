package com.actiongame.server.domain.progression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 技能树 (服务端权威)
 * 对应Unity SkillTree, 纯数据逻辑
 */
public class SkillTree {
    private final List<SkillTreeNode> nodes;
    private final Set<String> unlockedNodes = new HashSet<>();
    private int availableSkillPoints = 0;

    public SkillTree(List<SkillTreeNode> nodes) {
        this.nodes = nodes;
    }

    public void addSkillPoints(int amount) {
        availableSkillPoints += Math.max(0, amount);
    }

    public boolean canUnlock(String nodeId, int playerLevel) {
        SkillTreeNode node = findNode(nodeId);
        if (node == null) return false;
        if (unlockedNodes.contains(nodeId)) return false;
        if (playerLevel < node.getRequiredLevel()) return false;
        if (availableSkillPoints < node.getCost()) return false;
        for (String prereqId : node.getPrerequisiteNodeIds()) {
            if (!unlockedNodes.contains(prereqId)) return false;
        }
        return true;
    }

    public boolean tryUnlock(String nodeId, int playerLevel) {
        if (!canUnlock(nodeId, playerLevel)) return false;
        SkillTreeNode node = findNode(nodeId);
        availableSkillPoints -= node.getCost();
        unlockedNodes.add(nodeId);
        return true;
    }

    public boolean isUnlocked(String nodeId) {
        return unlockedNodes.contains(nodeId);
    }

    public void clearUnlocks() {
        unlockedNodes.clear();
        availableSkillPoints = 0;
    }

    private SkillTreeNode findNode(String nodeId) {
        for (SkillTreeNode node : nodes) {
            if (node.getNodeId().equals(nodeId)) return node;
        }
        return null;
    }

    public List<SkillTreeNode> getNodes() { return nodes; }
    public int getAvailableSkillPoints() { return availableSkillPoints; }
    public Set<String> getUnlockedNodes() { return unlockedNodes; }
}
