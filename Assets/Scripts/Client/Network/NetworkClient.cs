using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using UnityEngine;
using ActionGameDemo.Protocol;

namespace ActionGameDemo.Client
{
    /// <summary>
    /// 消息处理器委托
    /// </summary>
    public delegate void MessageHandler(IMessage message);

    /// <summary>
    /// 网络客户端 - 管理WebSocket连接、消息收发、心跳、协议版本协商
    /// </summary>
    public class NetworkClient : MonoBehaviour
    {
        [Header("Server Settings")]
        [SerializeField] private string m_serverHost = "127.0.0.1";
        [SerializeField] private int m_serverPort = 9090;
        [SerializeField] private int m_metricsPort = 9091;

        [Header("Protocol")]
        [SerializeField] private int m_protocolVersion = 10;

        [Header("Heartbeat")]
        [SerializeField] private float m_heartbeatInterval = 3f;

        [Header("Reconnection")]
        [SerializeField] private bool m_autoReconnect = true;
        [SerializeField] private float m_reconnectDelay = 2f;
        [SerializeField] private int m_maxReconnectAttempts = 5;

        private WebSocketClient m_webSocketClient;
        private float m_heartbeatTimer = 0f;
        private long m_sequenceId = 0;
        private int m_rtt = 0;
        private string m_playerId = "";

        private int m_reconnectAttempts = 0;
        private float m_reconnectTimer = 0f;
        private bool m_wasAuthenticated = false;
        private string m_lastPlayerName = "";
        private string m_lastRoomId = "";

        private const float REQUEST_TIMEOUT = 5f;
        private float m_loginTimer = 0f;
        private bool m_loginPending = false;
        private float m_joinRoomTimer = 0f;
        private bool m_joinRoomPending = false;
        private bool m_redirectPending = false;

        /// <summary>当前RTT (ms)</summary>
        public int Rtt => m_rtt;

        /// <summary>当前连接状态</summary>
        public ConnectionState State => m_webSocketClient?.State ?? ConnectionState.Disconnected;

        /// <summary>是否已认证</summary>
        public bool IsAuthenticated => State == ConnectionState.Authenticated;

        public string ServerHost => m_serverHost;

        public int ServerPort => m_serverPort;

        public int MetricsPort => m_metricsPort;

        public string PlayerId => m_playerId;

        public string RoomId => m_lastRoomId;

        /// <summary>连接成功事件</summary>
        public event Action OnConnected;

        /// <summary>断开连接事件</summary>
        public event Action<string> OnDisconnected;

        /// <summary>登录成功事件</summary>
        public event Action<LoginResp> OnLoginSuccess;

        /// <summary>登录失败事件</summary>
        public event Action<string> OnLoginFailed;

        /// <summary>加入房间结果事件</summary>
        public event Action<JoinRoomResp> OnJoinRoomResp;

        /// <summary>收到战斗帧快照事件</summary>
        public event Action<BattleFrameNotify> OnBattleFrame;

        /// <summary>战斗开始事件</summary>
        public event Action<BattleStartNotify> OnBattleStart;

        /// <summary>战斗结束事件</summary>
        public event Action<BattleEndNotify> OnBattleEnd;

        public event Action<MessageWrapper> OnAnyMessageReceived;

        public event Action<MessageId, byte[]> OnAnyMessageSent;

        public event Action<string> OnProtocolError;

        public event Action<PlayerActionResp> OnPlayerActionResp;

        private readonly Dictionary<MessageId, MessageHandler> m_handlers = new();

        public static NetworkClient Instance { get; private set; }

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
            m_webSocketClient = new WebSocketClient();
            m_webSocketClient.OnMessageReceived += OnRawMessageReceived;
            m_webSocketClient.OnConnected += HandleConnected;
            m_webSocketClient.OnDisconnected += HandleDisconnected;

            RegisterHandler(MessageId.LoginResp, HandleLoginResp);
            RegisterHandler(MessageId.HeartbeatResp, HandleHeartbeatResp);
            RegisterHandler(MessageId.JoinRoomResp, HandleJoinRoomResp);
            RegisterHandler(MessageId.BattleFrameNotify, HandleBattleFrame);
            RegisterHandler(MessageId.BattleStartNotify, HandleBattleStart);
            RegisterHandler(MessageId.BattleEndNotify, HandleBattleEnd);
            RegisterHandler(MessageId.PlayerActionResp, HandlePlayerActionResp);
        }

        private void Update()
        {
            m_webSocketClient?.Update();

            if (IsAuthenticated)
            {
                m_heartbeatTimer += Time.deltaTime;
                if (m_heartbeatTimer >= m_heartbeatInterval)
                {
                    m_heartbeatTimer = 0f;
                    SendHeartbeat();
                }
            }

            // 自动重连
            if (m_autoReconnect && m_wasAuthenticated && State == ConnectionState.Disconnected)
            {
                m_reconnectTimer += Time.deltaTime;
                if (m_reconnectTimer >= m_reconnectDelay && m_reconnectAttempts < m_maxReconnectAttempts)
                {
                    m_reconnectTimer = 0f;
                    m_reconnectAttempts++;
                    Debug.Log($"[NetworkClient] Auto-reconnect attempt {m_reconnectAttempts}/{m_maxReconnectAttempts}");
                    Connect();
                }
            }

            // 请求超时检测
            if (m_loginPending)
            {
                m_loginTimer += Time.deltaTime;
                if (m_loginTimer >= REQUEST_TIMEOUT)
                {
                    m_loginPending = false;
                    Debug.LogError("[NetworkClient] Login timeout");
                    OnLoginFailed?.Invoke("Login timeout");
                }
            }
            if (m_joinRoomPending)
            {
                m_joinRoomTimer += Time.deltaTime;
                if (m_joinRoomTimer >= REQUEST_TIMEOUT)
                {
                    m_joinRoomPending = false;
                    Debug.LogWarning("[NetworkClient] JoinRoom timeout");
                }
            }
        }

        private void OnDestroy()
        {
            _ = m_webSocketClient?.DisconnectAsync();
        }

        // === Public API ===

        public void RegisterHandler(MessageId messageId, MessageHandler handler)
        {
            if (m_handlers.ContainsKey(messageId))
                m_handlers[messageId] = handler;
            else
                m_handlers.Add(messageId, handler);
        }

        public void Configure(string host, int port, int metricsPort)
        {
            m_serverHost = host;
            m_serverPort = port;
            m_metricsPort = metricsPort;
        }

        public async void Connect()
        {
            try
            {
                await ConnectAsync();
            }
            catch (Exception ex)
            {
                Debug.LogError($"[NetworkClient] Connect error: {ex.Message}");
            }
        }

        private async Task ConnectAsync()
        {
            // 已在连接中, 不重复连接
            if (m_webSocketClient != null && m_webSocketClient.IsConnected)
                return;

            // 清理旧连接
            if (m_webSocketClient != null)
            {
                m_webSocketClient.OnMessageReceived -= OnRawMessageReceived;
                m_webSocketClient.OnConnected -= HandleConnected;
                m_webSocketClient.OnDisconnected -= HandleDisconnected;
                try { await m_webSocketClient.DisconnectAsync(); } catch {}
                m_webSocketClient = null;
            }

            m_webSocketClient = new WebSocketClient();
            m_webSocketClient.OnMessageReceived += OnRawMessageReceived;
            m_webSocketClient.OnConnected += HandleConnected;
            m_webSocketClient.OnDisconnected += HandleDisconnected;

            await m_webSocketClient.ConnectAsync(m_serverHost, m_serverPort);
        }

        public async void Disconnect()
        {
            try
            {
                await DisconnectAsync();
            }
            catch (Exception ex)
            {
                Debug.LogError($"[NetworkClient] Disconnect error: {ex.Message}");
            }
        }

        private async Task DisconnectAsync()
        {
            if (m_webSocketClient == null) return;
            await m_webSocketClient.DisconnectAsync();
        }

        public async void Login(string playerName, int characterConfigId = 1, string token = "demo-token")
        {
            try
            {
                await LoginAsync(playerName, characterConfigId, token);
            }
            catch (Exception ex)
            {
                Debug.LogError($"[NetworkClient] Login error: {ex.Message}");
            }
        }

        private async Task LoginAsync(string playerName, int characterConfigId = 1, string token = "demo-token")
        {
            m_lastPlayerName = playerName;
            m_loginPending = true;
            m_loginTimer = 0f;

            var req = new LoginReq
            {
                ProtocolVersion = m_protocolVersion,
                Token = token,
                PlayerName = playerName,
                CharacterConfigId = characterConfigId
            };

            await SendMessage(MessageId.LoginReq, req);
            Debug.Log($"[NetworkClient] Login sent: name={playerName}, version={m_protocolVersion}");
        }

        public async void JoinRoom(string playerId, string roomId = "")
        {
            try
            {
                await JoinRoomAsync(playerId, roomId);
            }
            catch (Exception ex)
            {
                Debug.LogError($"[NetworkClient] JoinRoom error: {ex.Message}");
            }
        }

        private async Task JoinRoomAsync(string playerId, string roomId = "")
        {
            m_lastRoomId = roomId;
            m_joinRoomPending = true;
            m_joinRoomTimer = 0f;

            var req = new JoinRoomReq
            {
                ProtocolVersion = m_protocolVersion,
                PlayerId = playerId,
                RoomId = roomId
            };

            await SendMessage(MessageId.JoinRoomReq, req);
        }

        public async void SendPlayerAction(int playerEntityId, long clientFrameIndex,
            PlayerActionData[] actions)
        {
            try
            {
                await SendPlayerActionAsync(playerEntityId, clientFrameIndex, actions);
            }
            catch (Exception ex)
            {
                Debug.LogError($"[NetworkClient] SendPlayerAction error: {ex.Message}");
            }
        }

        private async Task SendPlayerActionAsync(int playerEntityId, long clientFrameIndex,
            PlayerActionData[] actions)
        {
            var req = new PlayerActionReq
            {
                ProtocolVersion = m_protocolVersion,
                PlayerEntityId = playerEntityId,
                ClientFrameIndex = clientFrameIndex,
                Actions = actions
            };

            await SendMessage(MessageId.PlayerActionReq, req);
        }

        public async System.Threading.Tasks.Task<bool> SendMessage(MessageId messageId, IMessage message)
        {
            byte[] payload = message.ToByteArray();
            var wrapper = MessageWrapper.Wrap(messageId, ++m_sequenceId, m_protocolVersion, payload);
            bool ok = await m_webSocketClient.SendAsync(wrapper.ToByteArray());
            if (ok)
            {
                OnAnyMessageSent?.Invoke(messageId, payload);
            }
            return ok;
        }

        // === Internal Handlers ===

        private void HandleConnected()
        {
            Debug.Log("[NetworkClient] Connected to server");
            m_reconnectTimer = 0f;

            if (m_redirectPending)
            {
                m_redirectPending = false;
                if (!string.IsNullOrEmpty(m_lastPlayerName))
                {
                    Login(m_lastPlayerName);
                }
            }

            // 只有自动重连时才自动重新登录; 首次连接由调用方 (NetworkTestComponent等) 负责登录
            else if (m_wasAuthenticated && m_reconnectAttempts > 0 && !string.IsNullOrEmpty(m_lastPlayerName))
            {
                Login(m_lastPlayerName);
            }

            OnConnected?.Invoke();
        }

        private void HandleDisconnected(string reason)
        {
            m_heartbeatTimer = 0f;
            m_loginPending = false;
            m_joinRoomPending = false;
            Debug.LogWarning($"[NetworkClient] Disconnected: {reason}");
            OnDisconnected?.Invoke(reason);
        }

        private void OnRawMessageReceived(byte[] data)
        {
            try
            {
                var wrapper = MessageWrapper.ParseFrame(data);
                OnAnyMessageReceived?.Invoke(wrapper);

                // 协议版本校验
                if (wrapper.ProtocolVersion > 0 &&
                    (wrapper.ProtocolVersion < m_protocolVersion - 5 ||
                     wrapper.ProtocolVersion > m_protocolVersion + 10))
                {
                    Debug.LogError($"[NetworkClient] 协议版本不匹配: 服务端={wrapper.ProtocolVersion}, 客户端={m_protocolVersion}, 请更新客户端");
                    OnProtocolError?.Invoke(
                        $"Protocol version mismatch: server={wrapper.ProtocolVersion}, client={m_protocolVersion}");
                    OnDisconnected?.Invoke($"Protocol version mismatch: server={wrapper.ProtocolVersion}, client={m_protocolVersion}");
                    Disconnect();
                    return;
                }

                if (m_handlers.TryGetValue(wrapper.MessageId, out var handler))
                {
                    IMessage msg = CreateMessage(wrapper.MessageId);
                    if (msg != null && wrapper.Payload != null && wrapper.Payload.Length > 0)
                    {
                        msg.FromByteArray(wrapper.Payload);
                        handler?.Invoke(msg);
                    }
                }
                else
                {
                    Debug.LogWarning($"[NetworkClient] No handler for {wrapper.MessageId}");
                }
            }
            catch (Exception ex)
            {
                Debug.LogError($"[NetworkClient] Parse message error: {ex.Message}");
                OnProtocolError?.Invoke($"Parse message error: {ex.Message}");
            }
        }

        private void HandleLoginResp(IMessage msg)
        {
            var resp = (LoginResp)msg;
            m_loginPending = false;

            if (resp.Code == 0)
            {
                m_playerId = resp.PlayerId;
                m_webSocketClient.SetAuthenticated();
                m_wasAuthenticated = true;
                m_reconnectAttempts = 0;
                Debug.Log($"[NetworkClient] Login success! playerId={resp.PlayerId}, sessionKey={resp.SessionKey}");

                // 重连后自动重新加入房间
                if (!string.IsNullOrEmpty(m_lastRoomId))
                {
                    Debug.Log($"[NetworkClient] Rejoining room after reconnect: {m_lastRoomId}");
                    JoinRoom(resp.PlayerId, m_lastRoomId);
                }

                OnLoginSuccess?.Invoke(resp);
            }
            else
            {
                m_wasAuthenticated = false;
                Debug.LogError($"[NetworkClient] Login failed: {resp.Message}");
                OnLoginFailed?.Invoke(resp.Message);
            }
        }

        private void HandleHeartbeatResp(IMessage msg)
        {
            var resp = (HeartbeatResp)msg;
            m_rtt = resp.Rtt;
            Debug.Log($"[NetworkClient] Heartbeat RTT: {m_rtt}ms");
        }

        private void HandleJoinRoomResp(IMessage msg)
        {
            var resp = (JoinRoomResp)msg;
            m_joinRoomPending = false;
            if (resp.Code == 0 && !string.IsNullOrEmpty(resp.RoomId))
            {
                m_lastRoomId = resp.RoomId;
            }
            if (resp.Code == -3 && !string.IsNullOrEmpty(resp.RedirectAddress))
            {
                m_lastRoomId = resp.RoomId;
                Debug.LogWarning($"[NetworkClient] Room redirected to node {resp.RedirectNodeId} at {resp.RedirectAddress}");
                OnJoinRoomResp?.Invoke(resp);
                RedirectTo(resp.RedirectAddress);
                return;
            }
            Debug.Log($"[NetworkClient] JoinRoom: code={resp.Code}, roomId={resp.RoomId}, entityId={resp.PlayerEntityId}");
            OnJoinRoomResp?.Invoke(resp);
        }

        private async void RedirectTo(string address)
        {
            try
            {
                await RedirectToAsync(address);
            }
            catch (Exception ex)
            {
                Debug.LogError($"[NetworkClient] Redirect error: {ex.Message}");
            }
        }

        private async Task RedirectToAsync(string address)
        {
            string host = address;
            int port = m_serverPort;
            int colon = address.LastIndexOf(':');
            if (colon > 0 && int.TryParse(address.Substring(colon + 1), out int parsedPort))
            {
                host = address.Substring(0, colon);
                port = parsedPort;
            }

            m_serverHost = host;
            m_serverPort = port;
            m_wasAuthenticated = false;
            m_reconnectAttempts = 0;
            m_redirectPending = true;

            if (m_webSocketClient != null)
            {
                await m_webSocketClient.DisconnectAsync();
            }
            Connect();
        }

        private void HandleBattleFrame(IMessage msg)
        {
            var frame = (BattleFrameNotify)msg;
            OnBattleFrame?.Invoke(frame);
        }

        private void HandleBattleStart(IMessage msg)
        {
            var notify = (BattleStartNotify)msg;
            Debug.Log($"[NetworkClient] BattleStart: roomId={notify.RoomId}, startFrame={notify.StartFrameIndex}");
            OnBattleStart?.Invoke(notify);
        }

        private void HandleBattleEnd(IMessage msg)
        {
            var notify = (BattleEndNotify)msg;
            Debug.Log($"[NetworkClient] BattleEnd: roomId={notify.RoomId}, result={notify.Result}, frames={notify.TotalFrames}");
            OnBattleEnd?.Invoke(notify);
        }

        private void HandlePlayerActionResp(IMessage msg)
        {
            var resp = (PlayerActionResp)msg;
            OnPlayerActionResp?.Invoke(resp);
            if (!resp.Accepted)
            {
                Debug.LogWarning($"[NetworkClient] Action rejected: {resp.RejectReason}");
            }
        }

        private async void SendHeartbeat()
        {
            var req = new HeartbeatReq
            {
                ProtocolVersion = m_protocolVersion,
                Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                PlayerId = m_playerId
            };

            try
            {
                bool ok = await SendMessage(MessageId.HeartbeatReq, req);
                if (!ok)
                {
                    Debug.LogWarning("[NetworkClient] Heartbeat send failed (socket not open)");
                }
            }
            catch (Exception ex)
            {
                Debug.LogError($"[NetworkClient] Heartbeat error: {ex.Message}");
            }
        }

        private static IMessage CreateMessage(MessageId messageId)
        {
            return messageId switch
            {
                MessageId.LoginReq => new LoginReq(),
                MessageId.LoginResp => new LoginResp(),
                MessageId.HeartbeatReq => new HeartbeatReq(),
                MessageId.HeartbeatResp => new HeartbeatResp(),
                MessageId.JoinRoomReq => new JoinRoomReq(),
                MessageId.JoinRoomResp => new JoinRoomResp(),
                MessageId.PlayerActionReq => new PlayerActionReq(),
                MessageId.PlayerActionResp => new PlayerActionResp(),
                MessageId.BattleFrameNotify => new BattleFrameNotify(),
                MessageId.BattleStartNotify => new BattleStartNotify(),
                MessageId.BattleEndNotify => new BattleEndNotify(),
                _ => null
            };
        }
    }
}
