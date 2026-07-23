using UnityEngine;
// 修复 CS0234 (L50/L90/L97): 本文件位于 ActionGameDemo.Enemy.Combat，代码中的 Combat.DamageInfo
// 被编译器优先解析为 ActionGameDemo.Enemy.Combat.DamageInfo（不存在）。
// 修复：补充 using ActionGameDemo.Combat，并将 Combat.DamageInfo 改为 DamageInfo。
using ActionGameDemo.Combat;

namespace ActionGameDemo.Enemy.Combat
{
    /// <summary>
    /// 敌人受击系统：处理受击反应、击退、硬直等逻辑。
    /// </summary>
    [RequireComponent(typeof(EnemyController))]
    public class EnemyHurtSystem : MonoBehaviour
    {
        [Header("受击参数")]
        [SerializeField, Tooltip("受击硬直时间（秒）")] private float _hurtStunDuration = 0.3f;
        [SerializeField, Tooltip("击退力度")] private float _knockbackForce = 3f;
        [SerializeField, Tooltip("是否播放受击动画")] private bool _playHurtAnimation = true;

        private EnemyController _enemyController;
        private Character.CharacterStats _stats;
        private Animator _animator;

        private bool _isInHurtState = false;
        private float _hurtEndTime = 0f;

        public bool IsInHurtState => _isInHurtState;

        private void Awake()
        {
            _enemyController = GetComponent<EnemyController>();
            if (_enemyController != null)
            {
                _stats = _enemyController.Stats;
                _animator = _enemyController.Anim;
            }
            
            Debug.Log($"[EnemyHurtSystem] 初始化完成 - 硬直时间: {_hurtStunDuration}, 击退力: {_knockbackForce}");
        }

        private void Update()
        {
            CheckHurtState();
        }

        /// <summary>
        /// 处理受击
        /// </summary>
        /// <param name="damageInfo">伤害信息</param>
        public void HandleHurt(DamageInfo damageInfo)
        {
            if (_enemyController.IsDead)
            {
                Debug.Log($"[EnemyHurtSystem] {gameObject.name} 已死亡，忽略受击");
                return;
            }

            Debug.Log($"[EnemyHurtSystem] {gameObject.name} 受到攻击 - 伤害: {damageInfo.finalDamage}, 攻击者: {damageInfo.attacker?.name ?? "未知"}");

            // 设置受击状态
            _isInHurtState = true;
            _hurtEndTime = Time.time + _hurtStunDuration;

            // 停止移动
            _enemyController.StopMoving();

            // 播放受击动画
            if (_playHurtAnimation && _animator != null)
            {
                _animator.SetTrigger("Hurt");
                Debug.Log($"[EnemyHurtSystem] 播放受击动画");
            }

            // 施加击退
            ApplyKnockback(damageInfo);

            // 更新状态机
            if (_enemyController.StateMachine != null)
            {
                _enemyController.StateMachine.ChangeState(EnemyStateType.Hurt);
                Debug.Log($"[EnemyHurtSystem] 切换到受击状态");
            }
        }

        /// <summary>
        /// 处理受击（简化版）
        /// </summary>
        public void HandleHurt()
        {
            DamageInfo dummyInfo = new DamageInfo();
            HandleHurt(dummyInfo);
        }

        /// <summary>
        /// 施加击退效果
        /// </summary>
        private void ApplyKnockback(DamageInfo damageInfo)
        {
            if (damageInfo.attacker == null)
            {
                Debug.LogWarning("[EnemyHurtSystem] 攻击者为空，无法计算击退方向");
                return;
            }

            Vector3 knockbackDir = (transform.position - damageInfo.attacker.transform.position).normalized;
            knockbackDir.y = 0.5f; // 添加向上的力，产生浮空效果

            Debug.Log($"[EnemyHurtSystem] 击退方向: {knockbackDir}, 力度: {_knockbackForce}");

            // 如果有 NavMeshAgent，使用简单的位置偏移
            if (_enemyController.Agent != null)
            {
                Vector3 newPosition = transform.position + knockbackDir * _knockbackForce * 0.5f;
                _enemyController.Agent.Warp(newPosition);
                Debug.Log($"[EnemyHurtSystem] 已应用击退，新位置: {newPosition}");
            }
        }

        /// <summary>
        /// 检查受击状态是否结束
        /// </summary>
        private void CheckHurtState()
        {
            if (!_isInHurtState) return;

            if (Time.time >= _hurtEndTime)
            {
                _isInHurtState = false;
                Debug.Log($"[EnemyHurtSystem] {gameObject.name} 受击状态结束");
            }
        }

        /// <summary>
        /// 强制结束受击状态
        /// </summary>
        public void ForceExitHurt()
        {
            _isInHurtState = false;
            _hurtEndTime = 0f;
            Debug.Log($"[EnemyHurtSystem] {gameObject.name} 强制退出受击状态");
        }
    }
}