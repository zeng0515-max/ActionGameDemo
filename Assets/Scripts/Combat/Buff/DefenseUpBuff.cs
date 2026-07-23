namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// 防御提升 Buff：防御力提升 30%，持续 10s，取最高级。
    /// </summary>
    public class DefenseUpBuff : AttributeBuff
    {
        // 防御提升参数通过 BuffData 配置：
        //   attributeType = Defense
        //   attributeModifier = 30
        //   duration = 10s
    }
}
