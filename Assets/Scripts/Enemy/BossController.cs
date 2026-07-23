using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Combat;

namespace ActionGameDemo.Enemy
{
    /// <summary>
    /// Boss 阶段定义：HP 百分比阈值 + 该阶段属性倍率 + 可用技能。
    /// </summary>
    [System.Serializable]
    public class BossPhase
    {
        [Tooltip("阶段名称")] public string phaseName = "Phase 1";
        [Tooltip("HP 百分比阈值（低于此值触发）"), Range(0f, 1f)] public float hpThreshold = 0.5f;
        [Tooltip("攻击力倍率")] public float attackMultiplier = 1f;
        [Tooltip("移动速度倍率")] public float moveSpeedMultiplier = 1f;
        [Tooltip("攻击冷却倍率（越小越快）")] public float attackCooldownMultiplier = 1f;
        [Tooltip("是否狂暴")] public bool isEnrage = false;
    }

    /// <summary>
    /// Boss 控制器：继承 EnemyController，增加多阶段管理、狂暴模式。
    /// HP 到达阶段阈值时切换阶段，提升属性并改变攻击节奏。
    /// </summary>
    public class BossController : EnemyController
    {
        [Header("Boss 阶段配置")]
        [SerializeField] private List<BossPhase> _phases = new List<BossPhase>
        {
            new BossPhase { phaseName = "Phase 1", hpThreshold = 1f, attackMultiplier = 1f, moveSpeedMultiplier = 1f },
            new BossPhase { phaseName = "Phase 2", hpThreshold = 0.66f, attackMultiplier = 1.3f, moveSpeedMultiplier = 1.2f, attackCooldownMultiplier = 0.8f },
            new BossPhase { phaseName = "Phase 3 (Enrage)", hpThreshold = 0.33f, attackMultiplier = 2f, moveSpeedMultiplier = 1.5f, attackCooldownMultiplier = 0.5f, isEnrage = true }
        };

        [Header("Boss 特有")]
        [SerializeField, Tooltip("阶段切换时无敌时间（秒）")] private float _phaseSwitchInvincibilityTime = 1f;
        [SerializeField, Tooltip("Boss 等级（用于经验奖励）")] private int _bossLevel = 10;
        [SerializeField, Tooltip("击败 Boss 给予的经验值")] private float _expReward = 500f;

        private int _currentPhaseIndex = 0;
        private bool _isEnraged = false;

        /// <summary>当前阶段索引</summary>
        public int CurrentPhaseIndex => _currentPhaseIndex;
        /// <summary>当前阶段</summary>
        public BossPhase CurrentPhase => _phases.Count > 0 ? _phases[_currentPhaseIndex] : null;
        /// <summary>是否狂暴</summary>
        public bool IsEnraged => _isEnraged;
        /// <summary>经验奖励</summary>
        public float ExpReward => _expReward;

        /// <summary>阶段切换事件</summary>
        public event System.Action<int, BossPhase> OnPhaseChanged;

        protected override void Awake()
        {
            base.Awake();
            // 按 HP 阈值降序排列阶段
            _phases.Sort((a, b) => b.hpThreshold.CompareTo(a.hpThreshold));
        }

        protected override void Start()
        {
            base.Start();
        }

        /// <summary>
        /// 每帧检查阶段切换。
        /// </summary>
        private void LateUpdate()
        {
            CheckPhaseTransition();
        }

        /// <summary>检查是否需要切换阶段</summary>
        private void CheckPhaseTransition()
        {
            if (IsDead || Stats == null || _phases.Count == 0) return;

            float hpPercent = Stats.HealthPercent;
            int targetPhase = _currentPhaseIndex;

            for (int i = _currentPhaseIndex + 1; i < _phases.Count; i++)
            {
                if (hpPercent <= _phases[i].hpThreshold)
                {
                    targetPhase = i;
                }
                else
                {
                    break;
                }
            }

            if (targetPhase != _currentPhaseIndex)
            {
                TransitionToPhase(targetPhase);
            }
        }

        /// <summary>切换到指定阶段</summary>
        private void TransitionToPhase(int phaseIndex)
        {
            if (phaseIndex < 0 || phaseIndex >= _phases.Count) return;

            _currentPhaseIndex = phaseIndex;
            var phase = _phases[phaseIndex];

            // 应用阶段属性倍率
            if (EnemyData != null)
            {
                // 攻击力倍率通过临时 Buff 或直接修改 Stats 实现
                // 这里使用 Stats 修饰器
                if (phase.isEnrage && !_isEnraged)
                {
                    _isEnraged = true;
                    // 狂暴模式：2倍攻击、1.5倍速度
                    Stats?.AddAttackModifier(1f); // +100% attack
                }
            }

            // 阶段切换无敌
            if (_phaseSwitchInvincibilityTime > 0f)
            {
                SetInvincible(true);
                Invoke(nameof(RemovePhaseInvincibility), _phaseSwitchInvincibilityTime);
            }

            // 狂暴特效
            if (phase.isEnrage)
            {
                VFXEffectManager.Instance?.PlayEffectFollow(VFXEffectType.FireBurn, transform, Vector3.up);
            }

            OnPhaseChanged?.Invoke(phaseIndex, phase);

#if UNITY_EDITOR
            Debug.Log($"[BossController] {gameObject.name} 进入 {phase.phaseName} (阶段 {phaseIndex + 1}/{_phases.Count})");
#endif
        }

        private void RemovePhaseInvincibility()
        {
            SetInvincible(false);
        }

        /// <summary>获取当前阶段的攻击冷却倍率</summary>
        public float CurrentAttackCooldownMultiplier
        {
            get
            {
                if (CurrentPhase != null) return CurrentPhase.attackCooldownMultiplier;
                return 1f;
            }
        }

        protected override void Die()
        {
            // Boss 死亡时给予玩家经验
            var player = FindFirstObjectByType<Player.PlayerController>();
            if (player != null)
            {
                var levelSystem = player.GetComponent<Progression.LevelSystem>();
                if (levelSystem != null)
                {
                    levelSystem.AddExperience(_expReward);
                }
            }

#if UNITY_EDITOR
            Debug.Log($"[BossController] {gameObject.name} 被击败！给予 {_expReward} 经验。");
#endif

            base.Die();
        }

#if UNITY_EDITOR
        private void OnDrawGizmosSelected()
        {
            // 调用父类 Gizmos（通过 base 无法调用，这里省略）
            // 显示阶段信息
            if (_phases != null && _phases.Count > 0)
            {
                Gizmos.color = new Color(0.8f, 0f, 0.8f, 0.5f);
                Gizmos.DrawWireSphere(transform.position, 2f);
            }
        }
#endif
    }
}
