using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Client;
using ActionGameDemo.Protocol;
using ActionGameDemo.Combat;

namespace ActionGameDemo.Presentation
{
    /// <summary>
    /// 角色表现层 - 从服务端帧快照驱动表现
    /// 【架构红线】纯表现, 不做任何战斗判定, 不存储权威数值
    /// 事件消费由全局 BattleEventConsumer 处理, 此类只负责角色动画/位置
    /// </summary>
    public class CharacterPresentation : MonoBehaviour
    {
        [Header("Entity")]
        [SerializeField] private int m_entityId = -1;
        [SerializeField] private bool m_isLocalPlayer = false;
        [Tooltip("用于自动匹配 entityId: 在帧快照中按此索引选取 (0=player, 1-3=monster, 4=boss)")]
        [SerializeField] private int m_autoMatchIndex = -1;

        [Header("Animation")]
        [SerializeField] private Animator m_animator;
        [SerializeField] private string m_speedParam = "Speed";
        [SerializeField] private string m_stateParam = "State";

        [Header("Interpolation")]
        [SerializeField] private float m_interpSpeed = 10f;
        [Header("Local Player Reconciliation")]
        [Tooltip("本地玩家与服务端位置偏差超过此距离时, 向服务端位置拉回")]
        [SerializeField] private float m_reconcileThreshold = 0.5f;
        [Tooltip("拉回时的插值速度")]
        [SerializeField] private float m_reconcileSpeed = 15f;

        private Vector3 m_targetPosition;
        private Quaternion m_targetRotation;
        private Vector3 m_lastPosition;
        private CharacterState m_currentState = CharacterState.Idle;
        private bool m_initialized = false;

        public int EntityId => m_entityId;
        public bool IsLocalPlayer => m_isLocalPlayer;

        public void Initialize(int entityId, bool isLocalPlayer)
        {
            m_entityId = entityId;
            m_isLocalPlayer = isLocalPlayer;
            m_initialized = true;

            if (m_animator == null)
                m_animator = GetComponent<Animator>();

            if (FrameSnapshotManager.Instance != null)
            {
                m_targetPosition = FrameSnapshotManager.Instance.GetPosition(entityId);
            }
            else
            {
                m_targetPosition = transform.position;
            }

            m_targetRotation = transform.rotation;
            m_lastPosition = transform.position;
        }

        private void LateUpdate()
        {
            if (FrameSnapshotManager.Instance == null) return;

            // 自动匹配 entityId (通过帧快照中的实体顺序)
            if (!m_initialized && m_autoMatchIndex >= 0)
            {
                var allIds = new List<int>(FrameSnapshotManager.Instance.GetAllEntityIds());
                if (m_autoMatchIndex < allIds.Count)
                {
                    m_entityId = allIds[m_autoMatchIndex];
                    m_initialized = true;
                    if (m_animator == null)
                        m_animator = GetComponent<Animator>();
                    m_targetPosition = transform.position;
                    m_targetRotation = transform.rotation;
                    m_lastPosition = transform.position;
                    Debug.Log($"[CharPres] Auto-matched {gameObject.name} → entityId={m_entityId}");
                }
            }

            if (m_entityId < 0) return;

            var snapshotMgr = FrameSnapshotManager.Instance;

            if (!m_isLocalPlayer)
            {
                UpdateRemotePosition();
            }
            // 本地玩家位置由 NetworkInputBridge 全权负责, 此处不干预位置

            UpdateAnimationState(snapshotMgr);
        }

        private void UpdateRemotePosition()
        {
            if (FrameSnapshotManager.Instance.TryGetSnapshot(m_entityId, out var snap))
            {
                m_targetPosition = new Vector3(snap.Position.X, snap.Position.Y, snap.Position.Z);
                m_targetRotation = new Quaternion(snap.Rotation.X, snap.Rotation.Y, snap.Rotation.Z, snap.Rotation.W);

                float lerpFactor = Mathf.Clamp01(Time.deltaTime * m_interpSpeed);
                transform.position = Vector3.Lerp(transform.position, m_targetPosition, lerpFactor);
                transform.rotation = Quaternion.Slerp(transform.rotation, m_targetRotation, lerpFactor);
            }
        }

        private void ReconcileLocalPosition()
        {
            if (!FrameSnapshotManager.Instance.TryGetSnapshot(m_entityId, out var snap))
                return;

            var serverPos = new Vector3(snap.Position.X, snap.Position.Y, snap.Position.Z);
            float drift = Vector3.Distance(transform.position, serverPos);

            if (drift > m_reconcileThreshold)
            {
                float lerpFactor = Mathf.Clamp01(Time.deltaTime * m_reconcileSpeed);
                transform.position = Vector3.Lerp(transform.position, serverPos, lerpFactor);
            }
        }

        private void UpdateAnimationState(FrameSnapshotManager snapshotMgr)
        {
            CharacterState state = snapshotMgr.GetState(m_entityId);
            if (state != m_currentState)
            {
                m_currentState = state;
                if (m_animator != null)
                {
                    m_animator.SetInteger(m_stateParam, (int)state);
                }
            }

            if (m_animator != null && !m_isLocalPlayer)
            {
                float dt = Time.deltaTime;
                float speed = dt > 0f ? (transform.position - m_lastPosition).magnitude / dt : 0f;
                m_animator.SetFloat(m_speedParam, speed);
            }
            m_lastPosition = transform.position;
        }
    }
}
