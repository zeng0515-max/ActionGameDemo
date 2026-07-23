using UnityEngine;
using ActionGameDemo.Character;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// 护盾 Buff：吸收伤害，持续 15s，叠加护盾值。
    /// </summary>
    public class ShieldBuff : BuffBase
    {
        private float _shieldAmount;

        public float ShieldAmount => _shieldAmount;
        public bool IsShieldBroken => _shieldAmount <= 0f;

        public override void OnApply()
        {
            base.OnApply();
            _shieldAmount = _data.shieldValue * _stacks;

            var character = GetTargetCharacter();
            if (character != null)
            {
                character.AddShield(_shieldAmount);
            }
        }

        public override void OnRemove()
        {
            var character = GetTargetCharacter();
            if (character != null && _shieldAmount > 0f)
            {
                character.RemoveShield(_shieldAmount);
            }

            base.OnRemove();
        }

        public override void OnStack(int newStacks)
        {
            // 先移除旧护盾值
            var character = GetTargetCharacter();
            if (character != null && _shieldAmount > 0f)
            {
                character.RemoveShield(_shieldAmount);
            }

            base.OnStack(newStacks);

            // 添加新护盾值
            _shieldAmount = _data.shieldValue * newStacks;
            if (character != null)
            {
                character.AddShield(_shieldAmount);
            }
        }
    }
}
