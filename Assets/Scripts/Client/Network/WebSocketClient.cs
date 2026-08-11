using System;
using System.Collections.Generic;
using System.IO;
using System.Net.WebSockets;
using System.Threading;
using System.Threading.Tasks;
using UnityEngine;

namespace ActionGameDemo.Client
{
    /// <summary>
    /// 连接状态
    /// </summary>
    public enum ConnectionState
    {
        Disconnected,
        Connecting,
        Connected,
        Authenticating,
        Authenticated,
    }

    /// <summary>
    /// WebSocket客户端 - 管理与服务端的WebSocket连接
    /// </summary>
    public class WebSocketClient
    {
        private ClientWebSocket m_webSocket;
        private string m_serverUrl;
        private volatile ConnectionState m_state = ConnectionState.Disconnected;
        private readonly object m_stateLock = new object();

        private CancellationTokenSource m_cancellationTokenSource;

        // 接收缓冲区
        private byte[] m_receiveBuffer = new byte[64 * 1024];

        // 消息接收队列 (线程安全, 有上限防止网络抖动时内存暴涨)
        private const int MAX_QUEUE_SIZE = 256;
        private readonly Queue<byte[]> m_receiveQueue = new Queue<byte[]>();
        private readonly object m_queueLock = new object();

        // 断线事件队列 (从后台线程入队, 主线程 Update 出队派发)
        private string m_pendingDisconnectReason;
        private volatile bool m_disconnectPending;

        /// <summary>当前连接状态</summary>
        public ConnectionState State => m_state;

        /// <summary>是否已连接</summary>
        public bool IsConnected => m_state == ConnectionState.Connected || m_state == ConnectionState.Authenticated;

        /// <summary>
        /// 标记连接已通过认证 (由NetworkClient在登录成功后调用)
        /// </summary>
        public void SetAuthenticated()
        {
            lock (m_stateLock)
            {
                if (m_state == ConnectionState.Connected || m_state == ConnectionState.Authenticating)
                {
                    m_state = ConnectionState.Authenticated;
                }
            }
        }

        /// <summary>
        /// 重置为未认证状态 (由NetworkClient在断开或登出时调用)
        /// </summary>
        public void ResetAuth()
        {
            lock (m_stateLock)
            {
                if (m_state == ConnectionState.Authenticated)
                {
                    m_state = ConnectionState.Connected;
                }
            }
        }

        /// <summary>收到消息回调 (在主线程Update中触发)</summary>
        public event Action<byte[]> OnMessageReceived;

        /// <summary>连接断开回调</summary>
        public event Action<string> OnDisconnected;

        /// <summary>连接成功回调</summary>
        public event Action OnConnected;

        /// <summary>
        /// 连接到服务端
        /// </summary>
        public async Task<bool> ConnectAsync(string host, int port, string path = "/game")
        {
            if (m_state == ConnectionState.Connecting || m_state == ConnectionState.Connected)
            {
                Debug.LogWarning("[WebSocketClient] Already connecting/connected");
                return false;
            }

            m_serverUrl = $"ws://{host}:{port}{path}";
            lock (m_stateLock)
            {
                m_state = ConnectionState.Connecting;
            }

            try
            {
                m_cancellationTokenSource = new CancellationTokenSource();
                m_webSocket = new ClientWebSocket();

                Debug.Log($"[WebSocketClient] Connecting to {m_serverUrl}...");
                await m_webSocket.ConnectAsync(new Uri(m_serverUrl), m_cancellationTokenSource.Token);

                lock (m_stateLock)
                {
                    m_state = ConnectionState.Connected;
                }
                Debug.Log("[WebSocketClient] Connected!");
                OnConnected?.Invoke();

                _ = Task.Run(ReceiveLoopAsync);

                return true;
            }
            catch (Exception ex)
            {
                Debug.LogError($"[WebSocketClient] Connect failed: {ex.Message}");
                lock (m_stateLock)
                {
                    m_state = ConnectionState.Disconnected;
                }
                return false;
            }
        }

        /// <summary>
        /// 断开连接
        /// </summary>
        public async Task DisconnectAsync()
        {
            if (m_webSocket == null || m_state == ConnectionState.Disconnected)
                return;

            // 标记已断开, 阻止 ReceiveLoop 重复触发 OnDisconnected
            lock (m_stateLock)
            {
                m_state = ConnectionState.Disconnected;
            }

            try
            {
                m_cancellationTokenSource?.Cancel();

                if (m_webSocket.State == WebSocketState.Open)
                {
                    await m_webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Client disconnect",
                        CancellationToken.None);
                }
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"[WebSocketClient] Disconnect error: {ex.Message}");
            }
            finally
            {
                OnDisconnected?.Invoke("Client initiated disconnect");
                m_webSocket?.Dispose();
                m_webSocket = null;
            }
        }

        /// <summary>
        /// 发送二进制数据
        /// </summary>
        public async Task<bool> SendAsync(byte[] data)
        {
            if (m_webSocket == null || m_webSocket.State != WebSocketState.Open)
            {
                Debug.LogWarning("[WebSocketClient] Cannot send: not connected");
                return false;
            }

            try
            {
                await m_webSocket.SendAsync(new ArraySegment<byte>(data),
                    WebSocketMessageType.Binary, true, m_cancellationTokenSource.Token);
                return true;
            }
            catch (Exception ex)
            {
                Debug.LogError($"[WebSocketClient] Send failed: {ex.Message}");
                return false;
            }
        }

        /// <summary>
        /// 接收循环 (在后台线程运行, 支持分片消息)
        /// </summary>
        private async Task ReceiveLoopAsync()
        {
            var messageBuffer = new MemoryStream();

            try
            {
                while (m_webSocket.State == WebSocketState.Open)
                {
                    var result = await m_webSocket.ReceiveAsync(
                        new ArraySegment<byte>(m_receiveBuffer), m_cancellationTokenSource.Token);

                    if (result.MessageType == WebSocketMessageType.Close)
                    {
                        Debug.Log("[WebSocketClient] Server closed connection");
                        break;
                    }

                    // 累积分片消息
                    if (result.Count > 0)
                    {
                        messageBuffer.Write(m_receiveBuffer, 0, result.Count);
                    }

                    // 只在消息完整时入队
                    if (result.EndOfMessage)
                    {
                        byte[] data = messageBuffer.ToArray();
                        if (data.Length > 0)
                        {
                            lock (m_queueLock)
                            {
                                // 超过上限时丢弃最旧消息, 防止网络抖动导致内存暴涨
                                while (m_receiveQueue.Count >= MAX_QUEUE_SIZE)
                                {
                                    m_receiveQueue.Dequeue();
                                }
                                m_receiveQueue.Enqueue(data);
                            }
                        }
                        messageBuffer.SetLength(0);
                    }
                }
            }
            catch (OperationCanceledException)
            {
                // 正常取消
            }
            catch (Exception ex)
            {
                Debug.LogError($"[WebSocketClient] Receive error: {ex.Message}");
            }

            lock (m_stateLock)
            {
                // 已被 DisconnectAsync 标记为 Disconnected, 不重复触发
                if (m_state == ConnectionState.Disconnected)
                    return;
                m_state = ConnectionState.Disconnected;
            }
            // 不直接从后台线程触发事件, 而是入队等待主线程 Update 派发
            m_pendingDisconnectReason = "Receive loop ended";
            m_disconnectPending = true;
        }

        /// <summary>
        /// 在主线程Update中调用, 处理接收到的消息和断线事件
        /// </summary>
        public void Update()
        {
            while (true)
            {
                byte[] data;
                lock (m_queueLock)
                {
                    if (m_receiveQueue.Count == 0)
                        break;
                    data = m_receiveQueue.Dequeue();
                }
                OnMessageReceived?.Invoke(data);
            }

            // 主线程派发断线事件 (避免从后台线程触发 Unity API)
            if (m_disconnectPending)
            {
                m_disconnectPending = false;
                OnDisconnected?.Invoke(m_pendingDisconnectReason);
            }
        }
    }
}
