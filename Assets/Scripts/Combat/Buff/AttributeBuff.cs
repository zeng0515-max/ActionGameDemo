using ActionGameDemo.Character;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// 通用属性 Buff：支持任意属性增减（攻击力、防御力、移速、暴击率）。
    /// OnApply 时增加属性，OnRemove 时恢复。
    /// </summary>
    public class AttributeBuff : BuffBase
    {
        protected bool _isApplied;

        public override void OnApply()
        {
            base.OnApply();
            ApplyAttributeModifier();
        }

        public override void OnRemove()
        {
            RemoveAttributeModifier();
            base.OnRemove();
        }

        public override void OnStack(int newStacks)
        {
            // 先移除旧层数的效果，再应用新层数
            if (_isApplied)
            {
                RemoveAttributeModifier();
            }
            base.OnStack(newStacks);
            ApplyAttributeModifier();
        }

        protected virtual void ApplyAttributeModifier()
        {
            var stats = GetTargetStats();
            if (stats == null) return;

            float modifier = _data.attributeModifier / 100f * _stacks;

            switch (_data.attributeType)
            {
                case AttributeType.AttackPower:
                    stats.AddAttackModifier(modifier);
                    break;
                case AttributeType.Defense:
                    stats.AddDefenseModifier(modifier);
                    break;
                case AttributeType.MoveSpeed:
                    stats.AddMoveSpeedModifier(modifier);
                    break;
                case AttributeType.CriticalRate:
                    stats.AddCriticalRateModifier(modifier);
                    break;
            }

            _isApplied = true;
        }

        protected virtual void RemoveAttributeModifier()
        {
            if (!_isApplied) return;

            var stats = GetTargetStats();
            if (stats == null) return;

            float modifier = _data.attributeModifier / 100f * _stacks;

            switch (_data.attributeType)
            {
                case AttributeType.AttackPower:
                    stats.AddAttackModifier(-modifier);
                    break;
                case AttributeType.Defense:
                    stats.AddDefenseModifier(-modifier);
                    break;
                case AttributeType.MoveSpeed:
                    stats.AddMoveSpeedModifier(-modifier);
                    break;
                case AttributeType.CriticalRate:
                    stats.AddCriticalRateModifier(-modifier);
                    break;
            }

            _isApplied = false;
        }
    }
}
