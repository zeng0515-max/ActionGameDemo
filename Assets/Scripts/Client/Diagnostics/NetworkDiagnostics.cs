using System;
using System.Collections.Generic;
using ActionGameDemo.Protocol;
using UnityEngine;

namespace ActionGameDemo.Client.Diagnostics
{
    public enum DiagnosticLevel
    {
        Info = 0,
        Warn = 1,
        Error = 2
    }

    public sealed class ProtocolLogEntry
    {
        public double Time;
        public DiagnosticLevel Level;
        public string Text;
    }

    /// <summary>
    /// Network diagnostics for the validation console. Collects protocol counters,
    /// frame stream statistics, RTT samples and a bounded message log.
    /// </summary>
    public sealed class NetworkDiagnostics : MonoBehaviour
    {
        public static NetworkDiagnostics Instance { get; private set; }

        public long MessagesIn { get; private set; }
        public long MessagesOut { get; private set; }
        public long BytesIn { get; private set; }
        public long BytesOut { get; private set; }
        public long FramesReceived { get; private set; }
        public long FrameBytes { get; private set; }
        public long ActionsSent { get; private set; }
        public long ActionsAccepted { get; private set; }
        public long ActionsRejected { get; private set; }
        public long ProtocolErrors { get; private set; }
        public int ConnectCount { get; private set; }
        public int ReconnectCount { get; private set; }
        public int RedirectCount { get; private set; }
        public int CurrentFrameRate { get; private set; }
        public long LastFrameIndex { get; private set; }
        public string LastRoomId { get; private set; } = "";
        public float AverageRtt { get; private set; }
        public int MinRtt { get; private set; } = int.MaxValue;
        public int MaxRtt { get; private set; }
        public double ElapsedSeconds { get; private set; }

        private const int MaxLogEntries = 300;
        private readonly Queue<ProtocolLogEntry> m_log = new();
        private readonly List<int> m_rttSamples = new();
        private NetworkClient m_client;
        private bool m_subscribed;
        private int m_framesThisSecond;
        private float m_frameSampleTimer;
        private float m_rttSampleTimer;
        private bool m_everConnected;

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
        }

        private void Start()
        {
            m_client = NetworkClient.Instance;
            Attach();
            if (m_client == null)
            {
                Log(DiagnosticLevel.Warn, "NetworkClient not found; diagnostics will retry");
            }
        }

        private void Update()
        {
            ElapsedSeconds += Time.unscaledDeltaTime;

            if (m_client == null)
            {
                m_client = NetworkClient.Instance;
                if (m_client != null) Attach();
                return;
            }

            m_frameSampleTimer += Time.unscaledDeltaTime;
            if (m_frameSampleTimer >= 1f)
            {
                CurrentFrameRate = m_framesThisSecond;
                m_framesThisSecond = 0;
                m_frameSampleTimer -= 1f;
            }

            m_rttSampleTimer += Time.unscaledDeltaTime;
            if (m_rttSampleTimer >= 0.5f)
            {
                m_rttSampleTimer = 0f;
                int rtt = m_client.Rtt;
                if (rtt > 0)
                {
                    m_rttSamples.Add(rtt);
                    if (m_rttSamples.Count > 300) m_rttSamples.RemoveAt(0);

                    long sum = 0;
                    foreach (int sample in m_rttSamples) sum += sample;
                    AverageRtt = (float)sum / m_rttSamples.Count;
                    MinRtt = Math.Min(MinRtt, rtt);
                    MaxRtt = Math.Max(MaxRtt, rtt);
                }
            }
        }

        private void OnDestroy()
        {
            Detach();
            if (Instance == this) Instance = null;
        }

        public void Attach()
        {
            if (m_subscribed || m_client == null) return;
            m_subscribed = true;
            m_client.OnConnected += HandleConnected;
            m_client.OnDisconnected += HandleDisconnected;
            m_client.OnAnyMessageReceived += HandleMessageReceived;
            m_client.OnAnyMessageSent += HandleMessageSent;
            m_client.OnBattleFrame += HandleBattleFrame;
            m_client.OnPlayerActionResp += HandleActionResp;
            m_client.OnJoinRoomResp += HandleJoinRoomResp;
            m_client.OnProtocolError += HandleProtocolError;
            Log(DiagnosticLevel.Info, "Diagnostics attached");
        }

        public void Detach()
        {
            if (!m_subscribed || m_client == null) return;
            m_subscribed = false;
            m_client.OnConnected -= HandleConnected;
            m_client.OnDisconnected -= HandleDisconnected;
            m_client.OnAnyMessageReceived -= HandleMessageReceived;
            m_client.OnAnyMessageSent -= HandleMessageSent;
            m_client.OnBattleFrame -= HandleBattleFrame;
            m_client.OnPlayerActionResp -= HandleActionResp;
            m_client.OnJoinRoomResp -= HandleJoinRoomResp;
            m_client.OnProtocolError -= HandleProtocolError;
        }

        private void HandleConnected()
        {
            ConnectCount++;
            if (m_everConnected) ReconnectCount++;
            m_everConnected = true;
            Log(DiagnosticLevel.Info, "WebSocket connected");
        }

        private void HandleDisconnected(string reason)
        {
            Log(DiagnosticLevel.Warn, "Disconnected: " + reason);
        }

        private void HandleMessageReceived(MessageWrapper wrapper)
        {
            MessagesIn++;
            int payloadLen = wrapper.Payload?.Length ?? 0;
            BytesIn += 20 + payloadLen;
            if (wrapper.MessageId == MessageId.BattleFrameNotify)
            {
                FrameBytes += 20 + payloadLen;
            }
        }

        private void HandleMessageSent(MessageId messageId, byte[] payload)
        {
            MessagesOut++;
            int payloadLen = payload?.Length ?? 0;
            BytesOut += 20 + payloadLen;
            if (messageId == MessageId.PlayerActionReq)
            {
                ActionsSent++;
            }
        }

        private void HandleBattleFrame(BattleFrameNotify frame)
        {
            FramesReceived++;
            m_framesThisSecond++;
            LastFrameIndex = frame.FrameIndex;
            LastRoomId = frame.RoomId;
        }

        private void HandleActionResp(PlayerActionResp resp)
        {
            if (resp.Accepted)
            {
                ActionsAccepted++;
                Log(DiagnosticLevel.Info, $"Action accepted serverFrame={resp.ServerFrameIndex}");
            }
            else
            {
                ActionsRejected++;
                Log(DiagnosticLevel.Warn, $"Action rejected: {resp.RejectReason}");
            }
        }

        private void HandleJoinRoomResp(JoinRoomResp resp)
        {
            if (resp.Code == -3)
            {
                RedirectCount++;
                Log(DiagnosticLevel.Warn,
                    $"Room redirect to node={resp.RedirectNodeId} address={resp.RedirectAddress}");
            }
            else
            {
                Log(DiagnosticLevel.Info,
                    $"JoinRoom code={resp.Code} room={resp.RoomId} entity={resp.PlayerEntityId}");
            }
        }

        private void HandleProtocolError(string error)
        {
            ProtocolErrors++;
            Log(DiagnosticLevel.Error, error);
        }

        public void Log(DiagnosticLevel level, string text)
        {
            m_log.Enqueue(new ProtocolLogEntry
            {
                Time = Time.realtimeSinceStartup,
                Level = level,
                Text = text
            });
            while (m_log.Count > MaxLogEntries) m_log.Dequeue();
        }

        public string GetLogText(int maxLines)
        {
            var entries = m_log.ToArray();
            int start = Math.Max(0, entries.Length - maxLines);
            var lines = new List<string>(Math.Min(maxLines, entries.Length - start));
            for (int i = start; i < entries.Length; i++)
            {
                lines.Add(FormatLogEntry(entries[i]));
            }
            return string.Join("\n", lines);
        }

        public void Reset()
        {
            MessagesIn = 0;
            MessagesOut = 0;
            BytesIn = 0;
            BytesOut = 0;
            FramesReceived = 0;
            FrameBytes = 0;
            ActionsSent = 0;
            ActionsAccepted = 0;
            ActionsRejected = 0;
            ProtocolErrors = 0;
            ConnectCount = 0;
            ReconnectCount = 0;
            RedirectCount = 0;
            CurrentFrameRate = 0;
            LastFrameIndex = 0;
            LastRoomId = "";
            AverageRtt = 0f;
            MinRtt = int.MaxValue;
            MaxRtt = 0;
            m_log.Clear();
            m_rttSamples.Clear();
            m_framesThisSecond = 0;
            m_everConnected = false;
        }

        private static string FormatLogEntry(ProtocolLogEntry entry)
        {
            TimeSpan ts = TimeSpan.FromSeconds(entry.Time);
            string stamp = ts.ToString(@"hh\:mm\:ss");
            string prefix = entry.Level switch
            {
                DiagnosticLevel.Error => "ERR",
                DiagnosticLevel.Warn => "WRN",
                _ => "INF"
            };
            return $"[{stamp}] {prefix} {entry.Text}";
        }
    }
}
