using UnityEngine;

namespace ActionGameDemo.Combat
{
    /// <summary>
    /// 伤害信息结构体：传递完整伤害计算数据。
    /// </summary>
    public struct DamageInfo
    {
        public float baseDamage;
        public float skillMultiplier;
        public bool isCritical;
        public float criticalDamage;
        public float elementMultiplier;
        public float defense;
        public float finalDamage;
        public Vector3 hitPosition;
        public GameObject attacker;

        
        public ElementType attackElement;
        public ElementType defenderElement;

        // ---------- 公开只读属性 ----------
        /// <summary>最终伤害值（对外统一访问入口）</summary>
        public float DamageAmount => finalDamage;

        /// <summary>是否为有效伤害</summary>
        public bool IsValid => attacker != null || finalDamage > 0f;

        /// <summary>是否为元素攻击</summary>
        public bool IsElemental => attackElement != ElementType.None;

        /// <summary>是否克制</summary>
        public bool IsCountering => ElementSystem.IsCountering(attackElement, defenderElement);
    }
}
