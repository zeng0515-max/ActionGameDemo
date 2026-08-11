using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Protocol;

namespace ActionGameDemo.Client
{
    /// <summary>
    /// 帧快照管理器 - 接收并存储服务端帧快照, 提供查询接口供表现层读取
    /// 【架构红线】客户端不存储任何权威数值, 所有数据从最新帧快照实时读取
    /// </summary>
    public class FrameSnapshotManager : MonoBehaviour
    {
        public static FrameSnapshotManager Instance { get; private set; }

        public long CurrentFrameIndex { get; private set; }

        private const int MAX_PENDING_EVENTS = 500;
        private BattleFrameNotify m_latestFrame;

        private readonly Dictionary<int, CharacterSnapshot> m_latestSnapshots = new();
        private readonly Queue<DamageNumberEventData> m_pendingDamageEvents = new();
        private readonly Queue<VFXEventData> m_pendingVfxEvents = new();

        private bool m_battleActive = false;

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Start()
        {
            var client = NetworkClient.Instance;
            if (client != null)
            {
                client.OnBattleFrame += HandleBattleFrame;
                client.OnBattleStart += HandleBattleStart;
                client.OnBattleEnd += HandleBattleEnd;
            }
        }

        private void OnDestroy()
        {
            var client = NetworkClient.Instance;
            if (client != null)
            {
                client.OnBattleFrame -= HandleBattleFrame;
                client.OnBattleStart -= HandleBattleStart;
                client.OnBattleEnd -= HandleBattleEnd;
            }
        }

        private void HandleBattleFrame(BattleFrameNotify frame)
        {
            CurrentFrameIndex = frame.FrameIndex;
            m_latestFrame = frame;

            if (frame.Characters != null)
            {
                foreach (var snap in frame.Characters)
                {
                    m_latestSnapshots[snap.EntityId] = snap;
                }
            }

            if (frame.DamageEvents != null)
            {
                foreach (var dmg in frame.DamageEvents)
                {
                    if (m_pendingDamageEvents.Count < MAX_PENDING_EVENTS)
                        m_pendingDamageEvents.Enqueue(dmg);
                }
            }
            if (frame.VfxEvents != null)
            {
                foreach (var vfx in frame.VfxEvents)
                {
                    if (m_pendingVfxEvents.Count < MAX_PENDING_EVENTS)
                        m_pendingVfxEvents.Enqueue(vfx);
                }
            }
        }

        private void HandleBattleStart(BattleStartNotify notify)
        {
            m_battleActive = true;
            m_latestSnapshots.Clear();
            m_pendingDamageEvents.Clear();
            m_pendingVfxEvents.Clear();
            m_latestFrame = null;
            Debug.Log($"[FrameSnapshotManager] Battle started, frame={notify.StartFrameIndex}");
        }

        private void HandleBattleEnd(BattleEndNotify notify)
        {
            m_battleActive = false;
            Debug.Log($"[FrameSnapshotManager] Battle ended, result={notify.Result}");
        }

        // === 查询接口 ===

        public bool TryGetSnapshot(int entityId, out CharacterSnapshot snapshot)
        {
            return m_latestSnapshots.TryGetValue(entityId, out snapshot);
        }

        public IEnumerable<int> GetAllEntityIds()
        {
            return m_latestSnapshots.Keys;
        }

        public Vector3 GetPosition(int entityId)
        {
            if (m_latestSnapshots.TryGetValue(entityId, out var snap))
                return new Vector3(snap.Position.X, snap.Position.Y, snap.Position.Z);
            return Vector3.zero;
        }

        public CharacterState GetState(int entityId)
        {
            if (m_latestSnapshots.TryGetValue(entityId, out var snap))
                return snap.State;
            return CharacterState.Idle;
        }

        public float GetCurrentHealth(int entityId)
        {
            if (m_latestSnapshots.TryGetValue(entityId, out var snap))
                return snap.Stats.CurrentHealth;
            return 0f;
        }

        public float GetMaxHealth(int entityId)
        {
            if (m_latestSnapshots.TryGetValue(entityId, out var snap))
                return snap.Stats.MaxHealth;
            return 0f;
        }

        public int GetComboStep(int entityId)
        {
            if (m_latestSnapshots.TryGetValue(entityId, out var snap))
                return snap.ComboStep;
            return 0;
        }

        public BuffSnapshot[] GetBuffs(int entityId)
        {
            if (m_latestSnapshots.TryGetValue(entityId, out var snap))
                return snap.Buffs;
            return System.Array.Empty<BuffSnapshot>();
        }

        public List<DamageNumberEventData> ConsumeDamageEvents()
        {
            var events = new List<DamageNumberEventData>(m_pendingDamageEvents.Count);
            while (m_pendingDamageEvents.Count > 0)
                events.Add(m_pendingDamageEvents.Dequeue());
            return events;
        }

        public List<VFXEventData> ConsumeVFXEvents()
        {
            var events = new List<VFXEventData>(m_pendingVfxEvents.Count);
            while (m_pendingVfxEvents.Count > 0)
                events.Add(m_pendingVfxEvents.Dequeue());
            return events;
        }

        public BattleFrameNotify GetLatestFrame()
        {
            return m_latestFrame;
        }

        public bool IsBattleActive => m_battleActive;
    }
}
