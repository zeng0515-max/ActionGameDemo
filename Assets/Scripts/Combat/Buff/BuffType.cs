using UnityEngine;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// Buff 类型枚举和堆叠规则枚举。
    /// </summary>

    /// <summary>Buff 大类型分类</summary>
    public enum BuffType
    {
        AttributeBuff,   // 属性增减益（攻击提升、防御提升等）
        DotBuff,         // 持续伤害（灼烧、中毒等）
        ControlBuff,     // 控制效果（冰冻、眩晕等）
        ShieldBuff       // 护盾
    }

    /// <summary>属性类型（属性 Buff 用）</summary>
    public enum AttributeType
    {
        AttackPower,
        Defense,
        MoveSpeed,
        CriticalRate
    }

    /// <summary>Buff 堆叠规则</summary>
    public enum BuffStackingRule
    {
        /// <summary>刷新时间：相同 Buff 再次添加时刷新持续时间，不叠加层数</summary>
        RefreshDuration,
        /// <summary>叠加层数：相同 Buff 再次添加时层数+1（上限 maxStacks）</summary>
        StackStacks,
        /// <summary>取最高级：保留等级最高的 Buff</summary>
        TakeHighest
    }

    /// <summary>
    /// Buff 创建工厂：根据 BuffData 创建对应的 BuffBase 子类实例。
    /// </summary>
    public static class BuffFactory
    {
        public static BuffBase CreateBuff(BuffData data, GameObject target, GameObject source)
        {
            BuffBase buff = data.buffType switch
            {
                BuffType.DotBuff => CreateDotBuff(data, target, source),
                BuffType.AttributeBuff => CreateAttributeBuff(data, target, source),
                BuffType.ControlBuff => CreateControlBuff(data, target, source),
                BuffType.ShieldBuff => new ShieldBuff(),
                _ => new AttributeBuff()
            };

            buff.Initialize(data, target, source);
            return buff;
        }

        private static BuffBase CreateDotBuff(BuffData data, GameObject target, GameObject source)
        {
            // 根据 buffName 匹配具体 DoT 类型
            string nameLower = data.buffName != null ? data.buffName.ToLower() : "";
            if (nameLower.Contains("burn") || nameLower.Contains("灼烧"))
                return new BurnBuff();
            if (nameLower.Contains("poison") || nameLower.Contains("中毒"))
                return new PoisonBuff();
            return new DotBuff(); // 通用 DoT
        }

        private static BuffBase CreateAttributeBuff(BuffData data, GameObject target, GameObject source)
        {
            string nameLower = data.buffName != null ? data.buffName.ToLower() : "";
            if (nameLower.Contains("attack") || nameLower.Contains("攻击"))
                return new AttackUpBuff();
            if (nameLower.Contains("defense") || nameLower.Contains("防御"))
                return new DefenseUpBuff();
            return new AttributeBuff(); // 通用属性 Buff
        }

        private static BuffBase CreateControlBuff(BuffData data, GameObject target, GameObject source)
        {
            string nameLower = data.buffName != null ? data.buffName.ToLower() : "";
            if (nameLower.Contains("freeze") || nameLower.Contains("冰冻"))
                return new FreezeBuff();
            if (nameLower.Contains("shock") || nameLower.Contains("感电"))
                return new ShockBuff();
            return new FreezeBuff(); // 默认控制效果
        }
    }
}
