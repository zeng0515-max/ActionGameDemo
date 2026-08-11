package com.actiongame.server.domain.progression;

import java.util.List;

/**
 * 技能树节点 (服务端权威)
 * 对应Unity SkillTreeNodeData, 纯数据
 */
public class SkillTreeNode {
    private final String nodeId;
    private final String displayName;
    private final String description;
    private final int requiredLevel;
    private final int cost;
    private final List<String> prerequisiteNodeIds;
    private final float attackBonusPercent;
    private final float defenseBonusPercent;
    private final float healthBonusPercent;
    private final float criticalRateBonus;

    public SkillTreeNode(String nodeId, String displayName, String description,
                         int requiredLevel, int cost, List<String> prerequisiteNodeIds,
                         float attackBonusPercent, float defenseBonusPercent,
                         float healthBonusPercent, float criticalRateBonus) {
        this.nodeId = nodeId;
        this.displayName = displayName;
        this.description = description;
        this.requiredLevel = requiredLevel;
        this.cost = cost;
        this.prerequisiteNodeIds = prerequisiteNodeIds;
        this.attackBonusPercent = attackBonusPercent;
        this.defenseBonusPercent = defenseBonusPercent;
        this.healthBonusPercent = healthBonusPercent;
        this.criticalRateBonus = criticalRateBonus;
    }

    public String getNodeId() { return nodeId; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getCost() { return cost; }
    public List<String> getPrerequisiteNodeIds() { return prerequisiteNodeIds; }
    public float getAttackBonusPercent() { return attackBonusPercent; }
    public float getDefenseBonusPercent() { return defenseBonusPercent; }
    public float getHealthBonusPercent() { return healthBonusPercent; }
    public float getCriticalRateBonus() { return criticalRateBonus; }
}
