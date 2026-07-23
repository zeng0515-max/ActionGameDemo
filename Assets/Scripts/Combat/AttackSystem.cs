using System.Collections.Generic;
using UnityEngine;
// 修复 CS0246 (L15/L29/L30): CharacterBase 和 CharacterStats 定义在 ActionGameDemo.Character 命名空间，
// 本文件位于 ActionGameDemo.Combat，需补充 using 引用。
using ActionGameDemo.Character;

namespace ActionGameDemo.Combat
{
    /// <summary>
    /// 攻击系统核心组件：管理攻击碰撞检测、伤害计算、连击系统。
    /// 设计要点：
    /// 1. 通过动画帧事件控制 Hitbox 的开启/关闭
    /// 2. 使用 LayerMask 精确过滤检测目标
    /// 3. 使用 HashSet 防止单次挥击重复伤害同一目标
    /// 4. 只有成功命中才增加连击计数
    /// 5. 攻击状态锁机制确保流畅连招
    /// </summary>
    [RequireComponent(typeof(CharacterBase))]
    public class AttackSystem : MonoBehaviour
    {
        [Header("攻击参数")]
        [SerializeField, Tooltip("攻击检测半径")] private float _attackRadius = 2f;
        [SerializeField, Tooltip("攻击检测中心偏移（相对角色）")] private Vector3 _hitboxOffset = new Vector3(0f, 1f, 0.5f);
        [SerializeField, Tooltip("敌人层级掩码")] private LayerMask _enemyLayerMask = 1 << 9;
        [SerializeField, Tooltip("是否在编辑器中显示攻击范围 Gizmo")] private bool _showGizmo = true;

        [Header("连击参数")]
        [SerializeField, Tooltip("最大连击数")] private int _maxCombo = 4;
        [SerializeField, Tooltip("连击超时时间（秒）")] private float _comboTimeout = 1.5f;
        [SerializeField, Tooltip("连击伤害递增倍率")] private float _comboDamageMultiplier = 0.1f;

        private CharacterBase _characterBase;
        private CharacterStats _stats;
        
        // Hitbox 状态
        private bool _hitboxActive;
        private readonly HashSet<int> _hitTargetsThisSwing = new HashSet<int>();
        
        // 连击状态
        private int _currentCombo = 0;
        private float _lastHitTime = 0f;
        
        // 攻击状态锁
        private bool _isAttacking = false;

        public bool IsHitboxActive => _hitboxActive;
        public int CurrentCombo => _currentCombo;
        public bool IsAttacking => _isAttacking;

        public event System.Action<int> OnComboChanged;
        public event System.Action<GameObject, DamageInfo> OnHitTarget;

        private void Awake()
        {
            _characterBase = GetComponent<CharacterBase>();
            if (_characterBase != null)
                _stats = _characterBase.Stats;
            
            Debug.Log($"[AttackSystem] 初始化完成 - 攻击半径: {_attackRadius}, 层级掩码: {_enemyLayerMask}");
        }

        /// <summary>
        /// 动画帧事件：开启攻击碰撞盒
        /// 应在攻击动画的命中帧前调用
        /// </summary>
        public void EnableHitbox()
        {
            _hitboxActive = true;
            _hitTargetsThisSwing.Clear();
            _isAttacking = true;
            Debug.Log($"[AttackSystem] Hitbox 已开启 - 目标列表已清空");
        }

        /// <summary>
        /// 动画帧事件：关闭攻击碰撞盒
        /// 应在攻击动画的命中帧后调用
        /// </summary>
        public void DisableHitbox()
        {
            _hitboxActive = false;
            _hitTargetsThisSwing.Clear();
            Debug.Log($"[AttackSystem] Hitbox 已关闭");
        }

        /// <summary>
        /// 动画帧事件：执行命中检测
        /// 应在攻击动画的实际命中帧调用
        /// </summary>
        /// <param name="damageMultiplier">技能伤害倍率</param>
        public void PerformHitDetection(float damageMultiplier = 1f)
        {
            if (!_hitboxActive)
            {
                Debug.LogWarning("[AttackSystem] Hitbox 未激活，跳过命中检测");
                return;
            }

            // 计算检测中心点
            Vector3 hitCenter = transform.position + transform.forward * _hitboxOffset.z + 
                               Vector3.up * _hitboxOffset.y + transform.right * _hitboxOffset.x;
            
            Debug.Log($"[AttackSystem] 开始命中检测 - 中心点: {hitCenter}, 半径: {_attackRadius}, 层级掩码: {_enemyLayerMask}");

            // 检测敌人
            Collider[] hits = Physics.OverlapSphere(hitCenter, _attackRadius, _enemyLayerMask);
            Debug.Log($"[AttackSystem] 检测到 {hits.Length} 个碰撞体");

            bool hasHit = false;

            foreach (Collider hit in hits)
            {
                // 跳过自身
                if (hit.gameObject == gameObject)
                {
                    Debug.Log($"[AttackSystem] 跳过自身碰撞体");
                    continue;
                }

                // 检查是否已命中过
                int targetId = hit.GetInstanceID();
                if (_hitTargetsThisSwing.Contains(targetId))
                {
                    Debug.Log($"[AttackSystem] 目标 {hit.gameObject.name} 已在本次挥击中被命中过，跳过");
                    continue;
                }

                // 检查目标是否可被伤害
                IDamageable damageable = hit.GetComponent<IDamageable>();
                if (damageable == null)
                {
                    Debug.LogWarning($"[AttackSystem] 碰撞体 {hit.gameObject.name} 没有 IDamageable 接口");
                    continue;
                }

                CharacterBase targetBase = hit.GetComponent<CharacterBase>();
                if (targetBase != null && targetBase.IsDead)
                {
                    Debug.Log($"[AttackSystem] 目标 {hit.gameObject.name} 已死亡，跳过");
                    continue;
                }

                // 标记为已命中
                _hitTargetsThisSwing.Add(targetId);
                hasHit = true;

                Debug.Log($"[AttackSystem] 命中目标: {hit.gameObject.name} (ID: {targetId})");

                // 计算伤害
                float attackPower = _stats != null ? _stats.EffectiveAttackPower : 10f;
                float comboBonus = 1f + (_currentCombo * _comboDamageMultiplier);
                float finalDamage = attackPower * damageMultiplier * comboBonus;

                Debug.Log($"[AttackSystem] 伤害计算 - 攻击力: {attackPower}, 技能倍率: {damageMultiplier}, 连击加成: {comboBonus}, 最终伤害: {finalDamage}");

                // 构建伤害信息
                DamageInfo damageInfo = new DamageInfo
                {
                    baseDamage = attackPower,
                    skillMultiplier = damageMultiplier,
                    attacker = gameObject,
                    hitPosition = hit.ClosestPoint(hitCenter),
                    attackElement = _characterBase != null ? _characterBase.GetElementType() : ElementType.None
                };

                // 应用伤害
                damageable.TakeDamage(damageInfo);

                // 触发事件
                OnHitTarget?.Invoke(hit.gameObject, damageInfo);

                Debug.Log($"[AttackSystem] 伤害已施加给 {hit.gameObject.name}");
            }

            // 只有实际命中敌人才增加连击计数
            if (hasHit)
            {
                UpdateCombo();
            }
            else
            {
                Debug.Log("[AttackSystem] 本次挥击未命中任何目标，连击不增加");
            }
        }

        /// <summary>
        /// 更新连击计数
        /// </summary>
        private void UpdateCombo()
        {
            float timeSinceLastHit = Time.time - _lastHitTime;
            
            if (timeSinceLastHit > _comboTimeout)
            {
                Debug.Log($"[AttackSystem] 连击超时 ({timeSinceLastHit:F2}s > {_comboTimeout}s)，重置连击");
                _currentCombo = 1;
            }
            else
            {
                _currentCombo = Mathf.Min(_currentCombo + 1, _maxCombo);
                Debug.Log($"[AttackSystem] 连击增加: {_currentCombo}/{_maxCombo}");
            }
            
            _lastHitTime = Time.time;
            OnComboChanged?.Invoke(_currentCombo);
        }

        /// <summary>
        /// 重置连击计数
        /// </summary>
        public void ResetCombo()
        {
            _currentCombo = 0;
            _lastHitTime = 0f;
            Debug.Log("[AttackSystem] 连击已重置");
            OnComboChanged?.Invoke(_currentCombo);
        }

        /// <summary>
        /// 动画帧事件：攻击动画结束
        /// 释放攻击状态锁
        /// </summary>
        public void OnAttackAnimationEnd()
        {
            _isAttacking = false;
            _hitboxActive = false;
            _hitTargetsThisSwing.Clear();
            Debug.Log("[AttackSystem] 攻击动画结束，状态锁已释放");
        }

        /// <summary>
        /// 检查是否可以开始新攻击
        /// </summary>
        public bool CanAttack()
        {
            bool can = !_isAttacking;
            Debug.Log($"[AttackSystem] 攻击可行性检查: {can} (IsAttacking: {_isAttacking})");
            return can;
        }

        /// <summary>
        /// 设置攻击状态锁
        /// </summary>
        public void SetAttackLock(bool locked)
        {
            _isAttacking = locked;
            Debug.Log($"[AttackSystem] 攻击状态锁设置为: {locked}");
        }

#if UNITY_EDITOR
        private void OnDrawGizmosSelected()
        {
            if (!_showGizmo) return;

            Vector3 hitCenter = transform.position + transform.forward * _hitboxOffset.z + 
                               Vector3.up * _hitboxOffset.y + transform.right * _hitboxOffset.x;

            Gizmos.color = _hitboxActive ? Color.red : Color.yellow;
            Gizmos.DrawWireSphere(hitCenter, _attackRadius);

            // 绘制偏移线
            Gizmos.color = Color.cyan;
            Gizmos.DrawLine(transform.position, hitCenter);
        }
#endif
    }
}