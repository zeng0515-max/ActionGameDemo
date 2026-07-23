using UnityEngine;

namespace ActionGameDemo.Character
{
    /// <summary>
        /// 角色属性组件：封装角色的核心数值，提供安全的读写接口与变更事件。
    /// 设计原则：
    ///   1. 所有字段通过属性暴露，修改时自动 Clamp 防止越界；
    ///   2. 属性变更时触发回调，支持 UI 绑定与事件通知（支持 UI 绑定与事件通知）。
    /// </summary>
    public class CharacterStats : MonoBehaviour
    {
        // ---------- 核心属性 ----------
        [Header("基础属性")]
        [SerializeField, Tooltip("最大生命值")] private float _maxHealth = 100f;
        [SerializeField, Tooltip("当前生命值")] private float _currentHealth = 100f;

        [Header("战斗属性")]
        [SerializeField, Tooltip("攻击力（用于伤害计算）")] private float _attackPower = 10f;
        [SerializeField, Tooltip("防御力（减免受到伤害）")] private float _defense = 5f;
        [SerializeField, Tooltip("移动速度（米/秒）")] private float _moveSpeed = 5f;
        [SerializeField, Tooltip("暴击率（0-1）")] private float _criticalRate = 0.1f;
        [SerializeField, Tooltip("暴击伤害倍率")] private float _criticalDamageMultiplier = 1.5f;

        // ----------
        private float _attackModifier = 0f;      // 攻击力百分比修饰符（0 = 无加成）
        private float _defenseModifier = 0f;     // 防御力百分比修饰符
        private float _moveSpeedModifier = 0f;   // 移速百分比修饰符
        private float _criticalRateModifier = 0f; // 暴击率百分比修饰符
        private float _damageTakenMultiplier = 1f; // 受伤倍率（感电等 Debuff 使用）

        // ---------- 属性封装 ----------
        public float MaxHealth
        {
            get => _maxHealth;
            private set => _maxHealth = Mathf.Max(1f, value); // 最小为 1，防止除零
        }

        public float CurrentHealth
        {
            get => _currentHealth;
            private set
            {
                _currentHealth = Mathf.Clamp(value, 0f, _maxHealth);
                OnHealthChanged?.Invoke(_currentHealth, _maxHealth);
            }
        }

        public float AttackPower
        {
            get => _attackPower;
            private set => _attackPower = Mathf.Max(0f, value);
        }

        public float Defense
        {
            get => _defense;
            private set => _defense = Mathf.Max(0f, value);
        }

        public float MoveSpeed
        {
            get => _moveSpeed;
            private set => _moveSpeed = Mathf.Max(0f, value);
        }

        // ----------
        /// <summary>有效攻击力（基础 + Buff 修饰符）</summary>
        public float EffectiveAttackPower => _attackPower * (1f + _attackModifier);

        /// <summary>有效防御力（基础 + Buff 修饰符）</summary>
        public float EffectiveDefense => _defense * (1f + _defenseModifier);

        /// <summary>有效移动速度（基础 × (1 + Buff 修饰符)）</summary>
        public float EffectiveMoveSpeed => _moveSpeed * (1f + _moveSpeedModifier);

        /// <summary>有效暴击率（基础 + Buff 修饰符）</summary>
        public float EffectiveCriticalRate => Mathf.Clamp01(_criticalRate + _criticalRateModifier);

        /// <summary>暴击伤害倍率</summary>
        public float CriticalDamageMultiplier => _criticalDamageMultiplier;

        /// <summary>受伤倍率（感电等 Debuff 使用，默认 1）</summary>
        public float DamageTakenMultiplier
        {
            get => _damageTakenMultiplier;
            set => _damageTakenMultiplier = Mathf.Max(0f, value);
        }

        /// <summary>生命值百分比（0-1）</summary>
        public float HealthPercent => _maxHealth > 0f ? _currentHealth / _maxHealth : 0f;

        /// <summary>是否满血</summary>
        public bool IsFullHealth => Mathf.Approximately(_currentHealth, _maxHealth);

        /// <summary>是否死亡（生命值为 0）</summary>
        public bool IsDead => _currentHealth <= 0f;

        // ---------- 事件 ----------
        /// <summary>生命值变更事件（currentHealth, maxHealth）</summary>
        public System.Action<float, float> OnHealthChanged;

        /// <summary>属性整体重算事件（用于装备更换、升级后刷新）</summary>
        public System.Action OnStatsRecalculated;

        // ---------- 初始化 ----------
        private void Awake()
        {
            // 确保当前生命值不超出上限（防止 Inspector 配置错误）
            _currentHealth = Mathf.Clamp(_currentHealth, 0f, _maxHealth);
        }

        // ---------- 公共方法 ----------
        /// <summary>直接设置最大生命值（同步调整当前生命值上限）</summary>
        public void SetMaxHealth(float value)
        {
            MaxHealth = value;
            // 当前生命不能超过新的上限
            CurrentHealth = _currentHealth;
        }

        /// <summary>直接设置当前生命值（自动 Clamp）</summary>
        public void SetCurrentHealth(float value)
        {
            CurrentHealth = value;
        }

        /// <summary>修改当前生命值（正值为治疗，负值为伤害）</summary>
        public void ModifyHealth(float delta)
        {
            CurrentHealth += delta;
        }

        /// <summary>设置攻击力</summary>
        public void SetAttackPower(float value)
        {
            AttackPower = value;
        }

        /// <summary>设置防御力</summary>
        public void SetDefense(float value)
        {
            Defense = value;
        }

        /// <summary>设置移动速度</summary>
        public void SetMoveSpeed(float value)
        {
            MoveSpeed = value;
        }

        /// <summary>
        /// 根据基础值和加成系数重算所有属性。
        /// 调用场景：装备更换、升级、Buff 生效/失效时。
        /// </summary>
        public void RecalculateStats(float baseHealth, float baseAttack, float baseDefense, float baseSpeed,
                                     float healthMultiplier = 1f, float attackMultiplier = 1f,
                                     float defenseMultiplier = 1f, float speedMultiplier = 1f)
        {
            MaxHealth = baseHealth * healthMultiplier;
            AttackPower = baseAttack * attackMultiplier;
            Defense = baseDefense * defenseMultiplier;
            MoveSpeed = baseSpeed * speedMultiplier;

            // 修复：重新计算后确保当前血量不超过新的上限
            _currentHealth = Mathf.Clamp(_currentHealth, 0f, _maxHealth);

            OnStatsRecalculated?.Invoke();
        }

        /// <summary>重置为满血状态（用于复活、关卡重置）</summary>
        public void ResetToFullHealth()
        {
            CurrentHealth = _maxHealth;
        }

        // ----------
        /// <summary>添加攻击力百分比修饰符（正为增益，负为减益）</summary>
        public void AddAttackModifier(float percentage)
        {
            _attackModifier += percentage;
            OnStatsRecalculated?.Invoke();
        }

        /// <summary>添加防御力百分比修饰符</summary>
        public void AddDefenseModifier(float percentage)
        {
            _defenseModifier += percentage;
            OnStatsRecalculated?.Invoke();
        }

        /// <summary>添加移速百分比修饰符</summary>
        public void AddMoveSpeedModifier(float percentage)
        {
            _moveSpeedModifier += percentage;
            OnStatsRecalculated?.Invoke();
        }

        /// <summary>添加暴击率百分比修饰符</summary>
        public void AddCriticalRateModifier(float percentage)
        {
            _criticalRateModifier += percentage;
            OnStatsRecalculated?.Invoke();
        }

        /// <summary>重置所有 Buff 修饰符</summary>
        public void ResetAllModifiers()
        {
            _attackModifier = 0f;
            _defenseModifier = 0f;
            _moveSpeedModifier = 0f;
            _criticalRateModifier = 0f;
            _damageTakenMultiplier = 1f;
            OnStatsRecalculated?.Invoke();
        }

        // ---------- 调试 ----------
        public override string ToString()
        {
            return $"HP:{_currentHealth:F0}/{_maxHealth:F0} ATK:{_attackPower:F0} DEF:{_defense:F0} SPD:{_moveSpeed:F1}";
        }
    }
}
