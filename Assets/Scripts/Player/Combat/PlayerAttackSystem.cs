using UnityEngine;
// 修复 CS0234 (L27/L40): 本文件位于 ActionGameDemo.Player.Combat，代码中的 Combat.AttackSystem
// 被编译器优先解析为 ActionGameDemo.Player.Combat.AttackSystem（不存在）。
// 修复：补充 using ActionGameDemo.Combat，并将 Combat.AttackSystem 改为 AttackSystem。
using ActionGameDemo.Combat;

namespace ActionGameDemo.Player.Combat
{
    /// <summary>
    /// 玩家攻击系统：管理玩家的攻击输入、连击、技能释放等。
    /// 与 PlayerController 配合使用，提供更清晰的战斗逻辑分离。
    /// </summary>
    [RequireComponent(typeof(PlayerController))]
    public class PlayerAttackSystem : MonoBehaviour
    {
        [Header("攻击参数")]
        [SerializeField, Tooltip("普攻伤害倍率")] private float _attackDamageMultiplier = 1f;
        [SerializeField, Tooltip("技能伤害倍率")] private float _skillDamageMultiplier = 2f;
        [SerializeField, Tooltip("大招伤害倍率")] private float _ultimateDamageMultiplier = 3f;

        [Header("冷却时间")]
        [SerializeField, Tooltip("普攻冷却时间（秒）")] private float _attackCooldown = 0.2f;
        [SerializeField, Tooltip("技能冷却时间（秒）")] private float _skillCooldown = 5f;
        [SerializeField, Tooltip("大招能量需求")] private float _ultimateEnergyCost = 100f;

        private PlayerController _playerController;
        private AttackSystem _attackSystem;
        private Animator _animator;

        private float _lastAttackTime = 0f;
        private float _skillCooldownEnd = 0f;

        public bool IsAttackReady => Time.time >= _lastAttackTime + _attackCooldown;
        public bool IsSkillReady => Time.time >= _skillCooldownEnd;
        public bool IsUltimateReady => _playerController != null && _playerController.IsUltimateReady;

        private void Awake()
        {
            _playerController = GetComponent<PlayerController>();
            _attackSystem = GetComponent<AttackSystem>();
            _animator = GetComponent<Animator>();

            Debug.Log($"[PlayerAttackSystem] 初始化完成");
        }

        /// <summary>
        /// 执行普攻
        /// </summary>
        public void PerformAttack()
        {
            if (!CanAttack())
            {
                Debug.Log("[PlayerAttackSystem] 无法攻击（冷却中或状态不允许）");
                return;
            }

            Debug.Log("[PlayerAttackSystem] 执行普攻");

            _lastAttackTime = Time.time;
            
            // 触发攻击动画
            if (_animator != null)
            {
                _animator.SetTrigger("Attack");
                Debug.Log("[PlayerAttackSystem] 触发攻击动画");
            }

            // 攻击状态锁由动画事件管理
        }

        /// <summary>
        /// 执行技能
        /// </summary>
        public void PerformSkill()
        {
            if (!IsSkillReady)
            {
                float remaining = _skillCooldownEnd - Time.time;
                Debug.Log($"[PlayerAttackSystem] 技能冷却中，剩余时间: {remaining:F2}s");
                return;
            }

            Debug.Log("[PlayerAttackSystem] 执行技能");

            _skillCooldownEnd = Time.time + _skillCooldown;
            
            // 消耗技能资源（如果有）
            if (_playerController != null)
            {
                _playerController.ConsumeSkillCooldown();
            }

            // 触发技能动画
            if (_animator != null)
            {
                _animator.SetTrigger("Skill");
                Debug.Log("[PlayerAttackSystem] 触发技能动画");
            }
        }

        /// <summary>
        /// 执行大招
        /// </summary>
        public void PerformUltimate()
        {
            if (!IsUltimateReady)
            {
                Debug.Log("[PlayerAttackSystem] 大招未就绪（能量不足）");
                return;
            }

            Debug.Log("[PlayerAttackSystem] 执行大招");

            // 消耗大招能量
            if (_playerController != null)
            {
                _playerController.ConsumeUltimateEnergy();
            }

            // 触发达招动画
            if (_animator != null)
            {
                _animator.SetTrigger("Ultimate");
                Debug.Log("[PlayerAttackSystem] 触发达招动画");
            }
        }

        /// <summary>
        /// 检查是否可以攻击
        /// </summary>
        public bool CanAttack()
        {
            bool isReady = IsAttackReady;
            bool isNotAttacking = _attackSystem == null || _attackSystem.CanAttack();
            bool isAlive = _playerController != null && !_playerController.IsDead;

            bool can = isReady && isNotAttacking && isAlive;
            Debug.Log($"[PlayerAttackSystem] 攻击可行性检查: Ready={isReady}, NotAttacking={isNotAttacking}, Alive={isAlive}, Result={can}");
            return can;
        }

        /// <summary>
        /// 动画帧事件：开启攻击碰撞盒
        /// </summary>
        public void EnableHitbox()
        {
            Debug.Log("[PlayerAttackSystem] 动画事件: EnableHitbox");
            _attackSystem?.EnableHitbox();
        }

        /// <summary>
        /// 动画帧事件：关闭攻击碰撞盒
        /// </summary>
        public void DisableHitbox()
        {
            Debug.Log("[PlayerAttackSystem] 动画事件: DisableHitbox");
            _attackSystem?.DisableHitbox();
        }

        /// <summary>
        /// 动画帧事件：普攻命中帧
        /// </summary>
        public void OnAttackHitFrame()
        {
            Debug.Log("[PlayerAttackSystem] 动画事件: OnAttackHitFrame");
            _attackSystem?.PerformHitDetection(_attackDamageMultiplier);
        }

        /// <summary>
        /// 动画帧事件：技能命中帧
        /// </summary>
        public void OnSkillHitFrame()
        {
            Debug.Log("[PlayerAttackSystem] 动画事件: OnSkillHitFrame");
            _attackSystem?.PerformHitDetection(_skillDamageMultiplier);
        }

        /// <summary>
        /// 动画帧事件：大招命中帧
        /// </summary>
        public void OnUltimateHitFrame()
        {
            Debug.Log("[PlayerAttackSystem] 动画事件: OnUltimateHitFrame");
            _attackSystem?.PerformHitDetection(_ultimateDamageMultiplier);
        }

        /// <summary>
        /// 动画帧事件：攻击动画结束
        /// </summary>
        public void OnAttackAnimationEnd()
        {
            Debug.Log("[PlayerAttackSystem] 动画事件: OnAttackAnimationEnd");
            _attackSystem?.OnAttackAnimationEnd();
        }

        /// <summary>
        /// 重置连击
        /// </summary>
        public void ResetCombo()
        {
            _attackSystem?.ResetCombo();
        }
    }
}