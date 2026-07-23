using System.Collections.Generic;
using UnityEngine;

namespace ActionGameDemo.Combat
{
    /// <summary>
        /// 元素克制系统：管理元素关系和倍率。
    /// 克制关系：火克冰、冰克雷、雷克水、水克火。
    /// 使用字典存储克制关系，O(1) 查询。
    /// </summary>
    public enum ElementType
    {
        None,
        Fire,
        Ice,
        Lightning,
        Water
    }

    public static class ElementSystem
    {
        /// <summary>克制倍率（150%）</summary>
        public const float COUNTER_MULTIPLIER = 1.5f;

        /// <summary>无克制时的倍率</summary>
        public const float NORMAL_MULTIPLIER = 1f;

        // 克制关系字典：(攻击元素, 防御元素) → 倍率
        private static readonly Dictionary<(ElementType, ElementType), float> CounterMap;

        static ElementSystem()
        {
            CounterMap = new Dictionary<(ElementType, ElementType), float>
            {
                // 火克冰
                { (ElementType.Fire, ElementType.Ice), COUNTER_MULTIPLIER },
                // 冰克雷
                { (ElementType.Ice, ElementType.Lightning), COUNTER_MULTIPLIER },
                // 雷克水
                { (ElementType.Lightning, ElementType.Water), COUNTER_MULTIPLIER },
                // 水克火
                { (ElementType.Water, ElementType.Fire), COUNTER_MULTIPLIER },
            };
        }

        /// <summary>查询攻击元素对防御元素的倍率</summary>
        public static float GetMultiplier(ElementType attackElement, ElementType defenderElement)
        {
            if (attackElement == ElementType.None || defenderElement == ElementType.None)
                return NORMAL_MULTIPLIER;

            if (CounterMap.TryGetValue((attackElement, defenderElement), out float multiplier))
                return multiplier;

            return NORMAL_MULTIPLIER;
        }

        /// <summary>是否克制</summary>
        public static bool IsCountering(ElementType attackElement, ElementType defenderElement)
        {
            if (attackElement == ElementType.None || defenderElement == ElementType.None)
                return false;

            return CounterMap.ContainsKey((attackElement, defenderElement));
        }

        /// <summary>获取元素的标识颜色（用于伤害飘字和闪色特效）</summary>
        public static Color GetElementColor(ElementType element)
        {
            return element switch
            {
                ElementType.Fire => new Color(1f, 0.4f, 0.2f),      // 橙红
                ElementType.Ice => new Color(0.4f, 0.7f, 1f),       // 冰蓝
                ElementType.Lightning => new Color(1f, 1f, 0.3f),   // 黄色
                ElementType.Water => new Color(0.2f, 0.5f, 1f),     // 深蓝
                _ => Color.white
            };
        }
    }
}
