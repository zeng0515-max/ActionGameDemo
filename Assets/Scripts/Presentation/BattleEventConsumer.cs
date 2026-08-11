using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Protocol;
using ActionGameDemo.Combat;
using ActionGameDemo.Client;

namespace ActionGameDemo.Presentation
{
    /// <summary>
    /// 全局战斗事件消费者 — 不依赖特定角色, 独立消费帧快照事件
    /// 【架构红线】纯表现, 不做判定
    /// </summary>
    public class BattleEventConsumer : MonoBehaviour
    {
        public static BattleEventConsumer Instance { get; private set; }

        [SerializeField] private UnityEngine.GameObject m_damageNumberPrefab; // Legacy, unused
        private VFXEffectManager m_vfxManager;

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Start()
        {
            m_vfxManager = VFXEffectManager.Instance;
        }

        private void LateUpdate()
        {
            if (FrameSnapshotManager.Instance == null || !FrameSnapshotManager.Instance.IsBattleActive)
                return;

            var damageEvents = FrameSnapshotManager.Instance.ConsumeDamageEvents();
            foreach (var dmg in damageEvents)
            {
                Vector3 worldPos = new Vector3(dmg.Position.X, dmg.Position.Y, dmg.Position.Z);
                ShowDamageNumber(worldPos, dmg.Damage, dmg.IsCritical, dmg.Element);
            }

            var vfxEvents = FrameSnapshotManager.Instance.ConsumeVFXEvents();
            foreach (var vfx in vfxEvents)
            {
                Vector3 worldPos = new Vector3(vfx.Position.X, vfx.Position.Y, vfx.Position.Z);
                Quaternion worldRot = new Quaternion(vfx.Rotation.X, vfx.Rotation.Y, vfx.Rotation.Z, vfx.Rotation.W);
                m_vfxManager?.PlayEffect((VFXEffectType)vfx.VfxType, worldPos, worldRot);
            }
        }

        private void ShowDamageNumber(Vector3 position, float damage, bool isCritical, ActionGameDemo.Protocol.ElementType element)
        {
            VFXEffectManager.Instance?.PlayEffect(
                isCritical ? VFXEffectType.CriticalHit : VFXEffectType.HitImpact,
                position, Quaternion.identity);
        }
    }
}
