using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Character;

namespace ActionGameDemo.Combat
{
    /// <summary>
        /// 碰撞检测组件：由动画事件控制开关，在关键帧打开检测，避免持续检测。
    /// 使用 Layer 过滤，只检测目标层；使用 OverlapSphere 而非 OnTriggerEnter。
    /// 同一动画帧内同一目标只命中一次（去重）。
    /// </summary>
    public class Hitbox : MonoBehaviour
    {
        [Header("检测参数")]
        [SerializeField, Tooltip("检测半径")] private float _hitRadius = 1.5f;
        [SerializeField, Tooltip("检测中心偏移")] private Vector3 _hitOffset = Vector3.forward * 0.5f + Vector3.up;
        [SerializeField, Tooltip("检测层（默认 Enemy 层）")] private LayerMask _hitLayerMask = 1 << 9; // Enemy = 9

        [Header("调试")]
        [SerializeField, Tooltip("是否显示检测范围 Gizmo")] private bool _showGizmo = true;

        private bool _isActive;
        private GameObject _attacker;
        private float _damageMultiplier = 1f;
        private ElementType _attackElement = ElementType.None;
        private readonly HashSet<Collider> _hitTargets = new HashSet<Collider>();

        /// <summary>命中事件（target, damageInfo）</summary>
        public System.Action<Collider, DamageInfo> OnHit;

        /// <summary>当前是否激活</summary>
        public bool IsActive => _isActive;

        /// <summary>设置攻击者信息</summary>
        public void Configure(GameObject attacker, float damageMultiplier, ElementType attackElement)
        {
            _attacker = attacker;
            _damageMultiplier = damageMultiplier;
            _attackElement = attackElement;
        }

        /// <summary>动画事件：打开 Hitbox（开始检测）</summary>
        public void EnableHitbox()
        {
            _isActive = true;
            _hitTargets.Clear();
            PerformDetection();
        }

        /// <summary>动画事件：关闭 Hitbox（停止检测）</summary>
        public void DisableHitbox()
        {
            _isActive = false;
            _hitTargets.Clear();
        }

        /// <summary>执行碰撞检测</summary>
        private void PerformDetection()
        {
            if (!_isActive || _attacker == null) return;

            Vector3 center = transform.position + transform.TransformDirection(_hitOffset);
            Collider[] hits = Physics.OverlapSphere(center, _hitRadius, _hitLayerMask);

            foreach (Collider hit in hits)
            {
                // 去重：同一动画帧内同一目标只命中一次
                if (_hitTargets.Contains(hit)) continue;
                // 排除自身
                if (hit.gameObject == _attacker) continue;

                _hitTargets.Add(hit);

                var damageable = hit.GetComponent<IDamageable>();
                if (damageable != null)
                {
                    DamageInfo info = new DamageInfo
                    {
                        baseDamage = 0f, // 由调用方填充
                        skillMultiplier = _damageMultiplier,
                        attackElement = _attackElement,
                        attacker = _attacker,
                        hitPosition = hit.ClosestPoint(center)
                    };
                    OnHit?.Invoke(hit, info);
                }
            }
        }

        private void OnDrawGizmosSelected()
        {
            if (!_showGizmo) return;
            Vector3 center = transform.position + transform.TransformDirection(_hitOffset);
            Gizmos.color = _isActive ? Color.red : Color.yellow;
            Gizmos.DrawWireSphere(center, _hitRadius);
        }
    }
}
