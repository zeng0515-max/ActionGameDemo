using System;
using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Protocol;
using ActionGameDemo.Client;
using ActionGameDemo.Combat;

namespace ActionGameDemo.Player
{
    /// <summary>
    /// 网络输入桥接器 - 采集PlayerInputHandler输入, 打包为PlayerActionReq发送给服务端
    /// 【架构红线】只采集输入意图, 不执行任何战斗判定逻辑
    /// 本地预测仅限移动位移, 服务端校正
    /// </summary>
    public class NetworkInputBridge : MonoBehaviour
    {
        [SerializeField] private PlayerInputHandler m_inputHandler;
        [SerializeField] private int m_entityId = -1;
        [SerializeField] private float m_moveSpeed = 5f;
        [SerializeField] private float m_sendInterval = 0.05f;

        private float m_sendTimer = 0f;
        private long m_clientFrameIndex = 0;
        private bool m_active = false;

        private Vector3 m_predictedPosition;

        [SerializeField] private float m_reconcileThreshold = 0.5f;
        [Tooltip("未连接服务器时是否启用本地移动 (离线测试用)")]
        [SerializeField] private bool m_offlineMode = true;

        [Header("Local Action Feedback")]
        [SerializeField] private Animator m_animator;
        [SerializeField] private float m_jumpSpeed = 4f;
        [SerializeField] private float m_dodgeDashSpeed = 12f;
        [SerializeField] private float m_dodgeDuration = 0.2f;

        private float m_verticalVelocity;
        private float m_dodgeTimer;
        private Vector3 m_dodgeDirection;
        private float m_groundY;

        public int EntityId => m_entityId;

        public void Initialize(int entityId, float moveSpeed)
        {
            m_entityId = entityId;
            m_moveSpeed = moveSpeed;
            m_active = true;
            m_predictedPosition = transform.position;
        }

        private void Start()
        {
            if (m_inputHandler == null)
                m_inputHandler = GetComponent<PlayerInputHandler>();
            if (m_animator == null)
                m_animator = GetComponent<Animator>();
            m_groundY = transform.position.y;

            // 离线模式: 未连接服务器时自动激活本地移动
            if (m_offlineMode && !m_active)
            {
                m_active = true;
                m_predictedPosition = transform.position;
            }
        }

        private void Update()
        {
            if (!m_active || m_inputHandler == null) return;

            bool isOnline = NetworkClient.Instance != null && NetworkClient.Instance.IsAuthenticated;

            // 本地预测移动 (即使未连接服务器也执行, 仅视觉)
            Vector2 moveInput = m_inputHandler.MoveInput;
            if (moveInput.sqrMagnitude > 0.01f)
            {
                Vector3 moveDir = new Vector3(moveInput.x, 0, moveInput.y).normalized;

                // 从服务端快照读取权威移速, 未连接时用本地默认值
                float effectiveSpeed = m_moveSpeed;
                if (isOnline && FrameSnapshotManager.Instance != null && m_entityId > 0)
                {
                    if (FrameSnapshotManager.Instance.TryGetSnapshot(m_entityId, out var snap))
                    {
                        effectiveSpeed = snap.Stats.MoveSpeed;
                    }
                }

                m_predictedPosition += moveDir * effectiveSpeed * Time.deltaTime;
            }

            ApplyLocalActionFeedback(Time.deltaTime, isOnline);

            // 服务端校正 (仅在线时执行)
            if (isOnline && FrameSnapshotManager.Instance != null && m_entityId > 0)
            {
                if (FrameSnapshotManager.Instance.TryGetSnapshot(m_entityId, out var snap))
                {
                    Vector3 serverPos = new Vector3(snap.Position.X, snap.Position.Y, snap.Position.Z);
                    float dist = Vector3.Distance(m_predictedPosition, serverPos);
                    if (dist > m_reconcileThreshold)
                    {
                        m_predictedPosition = serverPos;
                    }
                    else
                    {
                        m_predictedPosition = Vector3.Lerp(m_predictedPosition, serverPos, Time.deltaTime * 10f);
                    }
                }
            }

            transform.position = m_predictedPosition;

            // 网络发送 (仅在线时执行)
            if (!isOnline) return;

            m_sendTimer += Time.deltaTime;
            if (m_sendTimer >= m_sendInterval)
            {
                m_sendTimer = 0f;
                SendActions();
            }
        }

        private void ApplyLocalActionFeedback(float deltaTime, bool isOnline)
        {
            Vector2 moveInput = m_inputHandler.MoveInput;

            if (m_inputHandler.JumpPressed)
            {
                m_verticalVelocity = m_jumpSpeed;
                SetAnimatorTrigger("Jump");
                if (!isOnline) m_inputHandler.ConsumeJump();
            }

            if (m_inputHandler.DodgePressed)
            {
                m_dodgeDirection = moveInput.sqrMagnitude > 0.01f
                    ? new Vector3(moveInput.x, 0, moveInput.y).normalized
                    : transform.forward;
                m_dodgeTimer = m_dodgeDuration;
                SetAnimatorTrigger("Dodge");
                PlayActionEffect(ActionType.Dodge);
                if (!isOnline) m_inputHandler.ConsumeDodge();
            }

            if (m_inputHandler.AttackPressed)
            {
                SetAnimatorTrigger("Attack");
                PlayActionEffect(ActionType.Attack);
                if (!isOnline) m_inputHandler.ConsumeAttack();
            }

            if (m_inputHandler.SkillPressed)
            {
                SetAnimatorTrigger("Skill");
                PlayActionEffect(ActionType.Skill);
                if (!isOnline) m_inputHandler.ConsumeSkill();
            }

            if (m_inputHandler.UltimatePressed)
            {
                SetAnimatorTrigger("Ultimate");
                PlayActionEffect(ActionType.Ultimate);
                if (!isOnline) m_inputHandler.ConsumeUltimate();
            }

            if (m_dodgeTimer > 0f)
            {
                m_predictedPosition += m_dodgeDirection * m_dodgeDashSpeed * deltaTime;
                m_dodgeTimer -= deltaTime;
            }

            m_predictedPosition.y += m_verticalVelocity * deltaTime;
            m_verticalVelocity -= 9.81f * deltaTime;
            if (m_predictedPosition.y <= m_groundY)
            {
                m_predictedPosition.y = m_groundY;
                m_verticalVelocity = 0f;
            }
        }

        private void SetAnimatorTrigger(string triggerName)
        {
            if (m_animator == null || string.IsNullOrEmpty(triggerName)) return;
            foreach (var param in m_animator.parameters)
            {
                if (param.name == triggerName)
                {
                    m_animator.SetTrigger(triggerName);
                    return;
                }
            }
        }

        private void PlayActionEffect(ActionType actionType)
        {
            if (VFXEffectManager.Instance == null) return;

            VFXEffectType effectType = actionType switch
            {
                ActionType.Attack => VFXEffectType.Slash,
                ActionType.Skill => VFXEffectType.SkillCast,
                ActionType.Ultimate => VFXEffectType.UltimateCast,
                ActionType.Dodge => VFXEffectType.Dodge,
                _ => VFXEffectType.None
            };

            if (effectType != VFXEffectType.None)
            {
                VFXEffectManager.Instance.PlayEffect(effectType, transform.position, transform.rotation);
            }
        }

        private void SendActions()
        {
            var actions = new List<PlayerActionData>();
            long timestamp = System.DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

            Vector2 moveInput = m_inputHandler.MoveInput;

            // 移动操作 (只添加一次)
            if (moveInput.sqrMagnitude > 0.01f)
            {
                actions.Add(new PlayerActionData
                {
                    ActionType = ActionType.Move,
                    MoveX = moveInput.x,
                    MoveZ = moveInput.y,
                    Timestamp = timestamp
                });
            }

            if (m_inputHandler.JumpPressed)
            {
                actions.Add(new PlayerActionData { ActionType = ActionType.Jump, Timestamp = timestamp });
                m_inputHandler.ConsumeJump();
            }

            if (m_inputHandler.AttackPressed)
            {
                actions.Add(new PlayerActionData { ActionType = ActionType.Attack, Timestamp = timestamp });
                m_inputHandler.ConsumeAttack();
            }

            if (m_inputHandler.SkillPressed)
            {
                actions.Add(new PlayerActionData { ActionType = ActionType.Skill, SkillId = 1, Timestamp = timestamp });
                m_inputHandler.ConsumeSkill();
            }

            if (m_inputHandler.UltimatePressed)
            {
                actions.Add(new PlayerActionData { ActionType = ActionType.Ultimate, SkillId = 2, Timestamp = timestamp });
                m_inputHandler.ConsumeUltimate();
            }

            if (m_inputHandler.DodgePressed)
            {
                actions.Add(new PlayerActionData { ActionType = ActionType.Dodge, Timestamp = timestamp });
                m_inputHandler.ConsumeDodge();
            }

            if (actions.Count > 0)
            {
                m_clientFrameIndex++;
                NetworkClient.Instance.SendPlayerAction(m_entityId, m_clientFrameIndex, actions.ToArray());
            }
        }
    }
}
