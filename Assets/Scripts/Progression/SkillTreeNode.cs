using UnityEngine;
using System.Collections.Generic;

namespace ActionGameDemo.Progression
{
    [CreateAssetMenu(fileName = "SkillTreeNode", menuName = "ActionGameDemo/Skill Tree Node")]
    public class SkillTreeNodeData : ScriptableObject
    {
        [Tooltip("节点 ID")] public string nodeId = "node_0";
        [Tooltip("节点名称")] public string displayName = "New Skill";
        [Tooltip("描述")] [TextArea] public string description = "";
        [Tooltip("图标")] public Sprite icon;
        [Tooltip("所需等级")] public int requiredLevel = 1;
        [Tooltip("消耗技能点")] public int cost = 1;
        [Tooltip("前置节点 ID 列表")] public List<string> prerequisiteNodeIds = new List<string>();
        [Tooltip("效果描述")] public string effectDescription = "";

        [Header("效果")]
        public float attackBonusPercent = 0f;
        public float defenseBonusPercent = 0f;
        public float healthBonusPercent = 0f;
        public float criticalRateBonus = 0f;
    }
}
