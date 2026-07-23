namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// 攻击提升 Buff：攻击力提升 20%，持续 10s，取最高级。
    /// 继承 AttributeBuff，通过 BuffData 配置 attributeType = AttackPower, attributeModifier = 20。
    /// </summary>
    public class AttackUpBuff : AttributeBuff
    {
        // 继承 AttributeBuff 的所有逻辑
        // 攻击提升特有参数通过 BuffData 配置：
        //   attributeType = AttackPower
        //   attributeModifier = 20
        //   duration = 10s
        //   stackingRule = TakeHighest
    }
}
