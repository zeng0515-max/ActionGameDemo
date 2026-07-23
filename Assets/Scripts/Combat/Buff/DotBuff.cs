using UnityEngine;
using ActionGameDemo.Character;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// 持续伤害 Buff 基类：按 tick 间隔造成伤害。
    /// 伤害值 = 攻击力 × tickValuePercent%
    /// </summary>
    public class DotBuff : BuffBase
    {
        private float _tickTimer;

        public override void OnApply()
        {
            base.OnApply();
            _tickTimer = 0f;
        }

        public override void OnUpdate(float deltaTime)
        {
            base.OnUpdate(deltaTime);

            if (!IsActive) return;

            _tickTimer += deltaTime;
            if (_tickTimer >= _data.tickInterval)
            {
                _tickTimer = 0f;
                ApplyTickDamage();
            }
        }

        protected virtual void ApplyTickDamage()
        {
            CharacterStats sourceStats = null;
            if (_source != null)
                sourceStats = _source.GetComponent<CharacterStats>();

            float attackPower = sourceStats != null ? sourceStats.EffectiveAttackPower : 10f;
            float damage = attackPower * (_data.tickValuePercent / 100f);

            var target = GetTargetCharacter();
            if (target != null && !target.IsDead)
            {
                target.TakeDamage(damage);
#if UNITY_EDITOR
                Debug.Log($"[DotBuff] {_data.buffName} tick 伤害: {damage} → {_target.name}");
#endif
            }
        }
    }
}
