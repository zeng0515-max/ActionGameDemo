using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Character;

namespace ActionGameDemo.Progression
{
    [CreateAssetMenu(fileName = "SkillTree", menuName = "ActionGameDemo/Skill Tree")]
    public class SkillTree : ScriptableObject
    {
        [SerializeField] private List<SkillTreeNodeData> _nodes = new List<SkillTreeNodeData>();
        private HashSet<string> _unlockedNodes = new HashSet<string>();
        private int _availableSkillPoints = 0;

        public List<SkillTreeNodeData> Nodes => _nodes;
        public int AvailableSkillPoints => _availableSkillPoints;
        public IReadOnlyCollection<string> UnlockedNodeIds => _unlockedNodes;

        public event System.Action<string> OnNodeUnlocked;
        public event System.Action<int> OnSkillPointsChanged;

        public void AddSkillPoints(int amount)
        {
            _availableSkillPoints += Mathf.Max(0, amount);
            OnSkillPointsChanged?.Invoke(_availableSkillPoints);
        }

        public bool CanUnlock(string nodeId, int playerLevel)
        {
            var node = FindNode(nodeId);
            if (node == null) return false;
            if (_unlockedNodes.Contains(nodeId)) return false;
            if (playerLevel < node.requiredLevel) return false;
            if (_availableSkillPoints < node.cost) return false;

            foreach (var prereqId in node.prerequisiteNodeIds)
            {
                if (!_unlockedNodes.Contains(prereqId)) return false;
            }
            return true;
        }

        public bool TryUnlock(string nodeId, int playerLevel, CharacterStats stats)
        {
            if (!CanUnlock(nodeId, playerLevel)) return false;

            var node = FindNode(nodeId);
            _availableSkillPoints -= node.cost;
            _unlockedNodes.Add(nodeId);
            ApplyNodeEffects(node, stats);

            OnNodeUnlocked?.Invoke(nodeId);
            OnSkillPointsChanged?.Invoke(_availableSkillPoints);
            return true;
        }

        public bool IsUnlocked(string nodeId) => _unlockedNodes.Contains(nodeId);

        private SkillTreeNodeData FindNode(string nodeId)
        {
            return _nodes.Find(n => n.nodeId == nodeId);
        }

        private void ApplyNodeEffects(SkillTreeNodeData node, CharacterStats stats)
        {
            if (stats == null) return;
            if (node.attackBonusPercent != 0f) stats.AddAttackModifier(node.attackBonusPercent / 100f);
            if (node.defenseBonusPercent != 0f) stats.AddDefenseModifier(node.defenseBonusPercent / 100f);
            if (node.criticalRateBonus != 0f) stats.AddCriticalRateModifier(node.criticalRateBonus / 100f);
            if (node.healthBonusPercent != 0f)
            {
                float bonus = stats.MaxHealth * node.healthBonusPercent / 100f;
                stats.SetMaxHealth(stats.MaxHealth + bonus);
                stats.ModifyHealth(bonus);
            }
        }

        public void ClearUnlocks()
        {
            _unlockedNodes.Clear();
            _availableSkillPoints = 0;
        }

        public List<string> GetUnlockedNodeIds() => new List<string>(_unlockedNodes);
    }
}
