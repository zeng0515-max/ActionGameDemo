using ActionGameDemo.Character;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// 冰冻 Debuff：移速降低 50%，持续 3s，刷新时间。
    /// OnApply 时降低移速倍率，OnRemove 时恢复。
    /// </summary>
    public class FreezeBuff : BuffBase
    {
        private float _speedReduction = 0.5f; // 移速降低 50%

        public override void OnApply()
        {
            base.OnApply();

            var stats = GetTargetStats();
            if (stats != null)
            {
                // 降低移速 50%（乘以 0.5 的倍率，即 -0.5 的修饰符）
                stats.AddMoveSpeedModifier(-_speedReduction);
            }
        }

        public override void OnRemove()
        {
            var stats = GetTargetStats();
            if (stats != null)
            {
                // 恢复移速
                stats.AddMoveSpeedModifier(_speedReduction);
            }

            base.OnRemove();
        }
    }
}
