using ActionGameDemo.Character;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// 中毒 Debuff：每秒造成 3% 攻击力伤害，持续 6s，可叠加 3 层。
    /// </summary>
    public class PoisonBuff : DotBuff
    {
        protected override void ApplyTickDamage()
        {
            CharacterStats sourceStats = null;
            if (_source != null)
                sourceStats = _source.GetComponent<CharacterStats>();

            float attackPower = sourceStats != null ? sourceStats.EffectiveAttackPower : 10f;
            // 中毒伤害受层数影响
            float damage = attackPower * (_data.tickValuePercent / 100f) * _stacks;

            var target = GetTargetCharacter();
            if (target != null && !target.IsDead)
            {
                target.TakeDamage(damage);
            }
        }

        public override void OnStack(int newStacks)
        {
            base.OnStack(newStacks);
        }
    }
}
