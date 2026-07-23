using UnityEngine;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// Buff 配置数据（ScriptableObject）。
    /// 定义 Buff 的类型、持续时间、效果数值、堆叠规则等。
    /// </summary>
    [CreateAssetMenu(fileName = "BuffData", menuName = "ActionGameDemo/Buff Data")]
    public class BuffData : ScriptableObject
    {
        [Header("基础信息")]
        [Tooltip("Buff 名称")] public string buffName = "New Buff";
        [Tooltip("Buff 大类型")] public BuffType buffType = BuffType.AttributeBuff;
        [Tooltip("Buff 图标")] public Sprite icon;
        [Tooltip("Buff 描述")] [TextArea] public string description = "";

        [Header("持续时间与堆叠")]
        [Tooltip("持续时间（秒）")] public float duration = 5f;
        [Tooltip("堆叠规则")] public BuffStackingRule stackingRule = BuffStackingRule.RefreshDuration;
        [Tooltip("最大堆叠层数")] public int maxStacks = 1;

        [Header("效果数值")]
        [Tooltip("每秒 tick 间隔（DoT 用）")] public float tickInterval = 1f;
        [Tooltip("每 tick 伤害/治疗值百分比（基于攻击力）")] public float tickValuePercent = 5f;
        [Tooltip("属性类型（属性 Buff 用）")] public AttributeType attributeType = AttributeType.AttackPower;
        [Tooltip("属性修改百分比（正为增益，负为减益）")] public float attributeModifier = 20f;
        [Tooltip("护盾值（护盾 Buff 用）")] public float shieldValue = 100f;

        [Header("元素关联")]
        [Tooltip("关联元素类型（元素 Buff 用）")] public Combat.ElementType relatedElement = Combat.ElementType.None;
    }
}
