using ActionGameDemo.Character;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// 感电 Debuff：受到伤害增加 25%，持续 4s，刷新时间。
    /// OnApply 时设置 DamageTakenMultiplier，OnRemove 时恢复。
    /// </summary>
    public class ShockBuff : BuffBase
    {
        private float _damageTakenIncrease = 0.25f; // 受伤增加 25%

        public override void OnApply()
        {
            base.OnApply();

            var stats = GetTargetStats();
            if (stats != null)
            {
                stats.DamageTakenMultiplier += _damageTakenIncrease;
            }
        }

        public override void OnRemove()
        {
            var stats = GetTargetStats();
            if (stats != null)
            {
                stats.DamageTakenMultiplier -= _damageTakenIncrease;
            }

            base.OnRemove();
        }

        public override void OnStack(int newStacks)
        {
            // 感电不叠加层数（刷新时间），由 BuffSystem 处理
            base.OnStack(newStacks);
        }
    }
}
