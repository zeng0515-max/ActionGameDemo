using UnityEngine;

namespace ActionGameDemo.Combat
{
    /// <summary>
        /// 动画特效类型枚举，定义所有战斗所需的视觉特效类型。
    /// 用于 VFXEffectManager 索引对应的特效预制体。
    /// </summary>
    public enum VFXEffectType
    {
        None,
        HitImpact,       // 普通命中打击特效
        CriticalHit,    // 暴击命中特效
        Slash,           // 挥砍刀光特效
        SkillCast,      // 技能释放特效
        UltimateCast,   // 大招释放特效
        FireBurn,       // 火焰燃烧特效（BurnBuff）
        IceFreeze,      // 冰冻特效（FreezeBuff）
        Poison,         // 中毒特效（PoisonBuff）
        LightningShock, // 雷电感电特效（ShockBuff）
        Shield,         // 护盾特效（ShieldBuff）
        Death,          // 死亡特效
        Dodge,           // 闪避残影特效
        Heal,            // 治疗特效
        BuffAppear,      // Buff出现通用特效
    }

    /// <summary>
    /// VFX 工具类：提供 BuffData 到 VFXEffectType 的映射。
    /// </summary>
    public static class VFXEffectMapper
    {
        /// <summary>根据 BuffData 的 buffType 和 buffName 推断对应的特效类型</summary>
        public static VFXEffectType GetBuffEffectType(Buff.BuffData data)
        {
            if (data == null) return VFXEffectType.None;

            // 先按关联元素判断
            if (data.relatedElement != ElementType.None)
            {
                return data.relatedElement switch
                {
                    ElementType.Fire => VFXEffectType.FireBurn,
                    ElementType.Ice => VFXEffectType.IceFreeze,
                    ElementType.Lightning => VFXEffectType.LightningShock,
                    ElementType.Water => VFXEffectType.Poison, // 水元素暂复用中毒特效
                    _ => VFXEffectType.BuffAppear
                };
            }

            // 按 BuffType 判断
            switch (data.buffType)
            {
                case Buff.BuffType.DotBuff:
                    // 按 buffName 细分
                    string nameLower = data.buffName != null ? data.buffName.ToLower() : "";
                    if (nameLower.Contains("burn") || nameLower.Contains("火"))
                        return VFXEffectType.FireBurn;
                    if (nameLower.Contains("poison") || nameLower.Contains("毒"))
                        return VFXEffectType.Poison;
                    return VFXEffectType.FireBurn; // 默认 DoT 用火焰

                case Buff.BuffType.ControlBuff:
                    string ctrlName = data.buffName != null ? data.buffName.ToLower() : "";
                    if (ctrlName.Contains("freeze") || ctrlName.Contains("冰"))
                        return VFXEffectType.IceFreeze;
                    if (ctrlName.Contains("shock") || ctrlName.Contains("电"))
                        return VFXEffectType.LightningShock;
                    return VFXEffectType.IceFreeze;

                case Buff.BuffType.ShieldBuff:
                    return VFXEffectType.Shield;

                case Buff.BuffType.AttributeBuff:
                default:
                    return VFXEffectType.BuffAppear;
            }
        }

        /// <summary>根据 ElementType 获取对应的元素特效类型</summary>
        public static VFXEffectType GetElementEffectType(ElementType element)
        {
            return element switch
            {
                ElementType.Fire => VFXEffectType.FireBurn,
                ElementType.Ice => VFXEffectType.IceFreeze,
                ElementType.Lightning => VFXEffectType.LightningShock,
                ElementType.Water => VFXEffectType.Poison,
                _ => VFXEffectType.None
            };
        }
    }
}
