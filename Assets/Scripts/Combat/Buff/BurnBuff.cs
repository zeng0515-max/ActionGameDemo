using ActionGameDemo.Character;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// 灼烧 Debuff：每秒造成 5% 攻击力伤害，持续 5s，刷新时间不叠加层数。
    /// </summary>
    public class BurnBuff : DotBuff
    {
        // 继承 DotBuff 的所有逻辑
        // 灼烧特有参数通过 BuffData 配置：
        //   duration = 5s
        //   tickInterval = 1s
        //   tickValuePercent = 5%
    }
}
