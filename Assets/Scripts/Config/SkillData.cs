using UnityEngine;

namespace ActionGameDemo.Config
{
    [CreateAssetMenu(fileName = "SkillData", menuName = "ActionGameDemo/Skill Data")]
    public class SkillData : ScriptableObject
    {
        public string skillName;
        [Tooltip("伤害倍率")] public float damageMultiplier;
        [Tooltip("冷却时间（秒）")] public float cooldown;
        [Tooltip("攻击范围")] public float range;
        [Tooltip("连段索引")] public int comboIndex;
        [Tooltip("是否可取消（衔接下一段）")] public bool canCancel;
        [Tooltip("连段动画触发器名（如 Attack_01, Attack_02），留空则使用默认 Attack")] public string animationTriggerName = "Attack";
    }
}
