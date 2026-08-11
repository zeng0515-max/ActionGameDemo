using UnityEngine;
using ActionGameDemo.Protocol;

namespace ActionGameDemo.Client
{
    /// <summary>
    /// 网络连接测试组件 - 挂载到场景中测试WebSocket连接、登录、心跳
    /// 用法: 挂载到任意GameObject, 运行后按空格连接服务端
    /// </summary>
    public class NetworkTestComponent : MonoBehaviour
    {
        [SerializeField] private Transform m_playerTransform;
        private int m_entityId = -1;
        private float m_moveSpeed = 5f;

        private void Start()
        {
            var client = NetworkClient.Instance;
            client.OnConnected += () =>
            {
                Debug.Log("[Test] Connected! Sending login...");
                client.Login("TestPlayer", 1);
            };

            client.OnLoginSuccess += (resp) =>
            {
                Debug.Log($"[Test] Login OK: playerId={resp.PlayerId}");
                client.JoinRoom(resp.PlayerId);
            };

            client.OnJoinRoomResp += (resp) =>
            {
                if (resp.Code == 0)
                {
                    m_entityId = resp.PlayerEntityId;
                    Debug.Log($"[Test] JoinRoom OK: entityId={m_entityId}");
                    InitializeBridge();
                }
            };

            client.OnBattleStart += (notify) =>
            {
                Debug.Log($"[Test] Battle started! roomId={notify.RoomId}");
                // 如果 JoinRoomResp 没初始化 bridge, 在这里也尝试一次
                if (m_entityId >= 0) InitializeBridge();
            };

            client.OnBattleFrame += (frame) =>
            {
                // 从帧快照读取权威移速, 更新 bridge
                if (m_entityId >= 0 && frame.Characters != null && m_playerTransform != null)
                {
                    foreach (var snap in frame.Characters)
                    {
                        if (snap.EntityId == m_entityId)
                        {
                            m_moveSpeed = snap.Stats.MoveSpeed;
                            // 检查 bridge 是否已初始化, 没有则用帧数据初始化
                            var bridge = m_playerTransform.GetComponent("NetworkInputBridge");
                            if (bridge != null)
                            {
                                var activeField = bridge.GetType().GetField("m_active",
                                    System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
                                if (activeField != null && (bool)activeField.GetValue(bridge) == false)
                                {
                                    var method = bridge.GetType().GetMethod("Initialize");
                                    if (method != null)
                                    {
                                        method.Invoke(bridge, new object[] { m_entityId, m_moveSpeed });
                                        Debug.Log($"[Test] NetworkInputBridge initialized from frame, entityId={m_entityId}, moveSpeed={m_moveSpeed}");
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            };

            client.OnLoginFailed += (msg) =>
            {
                Debug.LogError($"[Test] Login failed: {msg}");
            };

            client.OnDisconnected += (reason) =>
            {
                Debug.LogWarning($"[Test] Disconnected: {reason}");
                m_entityId = -1;
            };

            Invoke(nameof(TryAutoConnect), 0.5f);
        }

        private void TryAutoConnect()
        {
            if (NetworkClient.Instance != null && !NetworkClient.Instance.IsAuthenticated)
            {
                Debug.Log("[Test] Auto-connecting to server...");
                NetworkClient.Instance.Connect();
            }
        }

        private void InitializeBridge()
        {
            if (m_playerTransform == null || m_entityId < 0) return;

            var bridge = m_playerTransform.GetComponent("NetworkInputBridge");
            if (bridge != null)
            {
                var method = bridge.GetType().GetMethod("Initialize");
                if (method != null)
                {
                    method.Invoke(bridge, new object[] { m_entityId, m_moveSpeed });
                    Debug.Log($"[Test] NetworkInputBridge initialized, entityId={m_entityId}, moveSpeed={m_moveSpeed}");
                }
            }
        }

        private void Update()
        {
            if (Input.GetKeyDown(KeyCode.F8))
            {
                if (NetworkClient.Instance.IsAuthenticated)
                    return;
                Debug.Log("[Test] Connecting to server...");
                NetworkClient.Instance.Connect();
            }

            if (Input.GetKeyDown(KeyCode.F9))
            {
                NetworkClient.Instance.Disconnect();
            }
        }
    }
}
