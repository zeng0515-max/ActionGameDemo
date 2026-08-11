using System;
using System.Collections;
using ActionGameDemo.Protocol;
using UnityEngine;
using UnityEngine.Networking;

namespace ActionGameDemo.Client.Diagnostics
{
    /// <summary>
    /// IMGUI validation console. It is created automatically at runtime so the
    /// existing scene does not need art/UI prefabs.
    /// </summary>
    public sealed class ServerValidationPanel : MonoBehaviour
    {
        public static ServerValidationPanel Instance { get; private set; }

        private NetworkClient m_client;
        private NetworkDiagnostics m_diagnostics;
        private AutoValidationRunner m_runner;

        private string m_host = "127.0.0.1";
        private string m_port = "9090";
        private string m_metricsPort = "9091";
        private string m_playerName = "ValidatorPlayer";
        private string m_playerId = "";
        private string m_roomId = "";
        private int m_entityId = -1;
        private string m_phase = "idle";
        private string m_report = "";
        private string m_httpHealth = "not checked";
        private string m_httpMetrics = "not checked";
        private string m_httpAlerts = "not checked";
        private Vector2 m_logScroll = Vector2.zero;
        private bool m_showPanel = true;
        private bool m_manualJoinPending;
        private bool m_styleReady;
        private long m_manualActionFrame;
        private GUIStyle m_titleStyle;
        private GUIStyle m_monoStyle;
        private Texture2D m_panelTexture;

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        private static void AutoCreate()
        {
            if (FindFirstObjectByType<ServerValidationPanel>() != null) return;
            var go = new GameObject("ServerValidationConsole");
            DontDestroyOnLoad(go);
            go.AddComponent<ServerValidationPanel>();
        }

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
            if (m_client != null)
            {
                m_host = m_client.ServerHost;
                m_port = m_client.ServerPort.ToString();
                m_metricsPort = m_client.MetricsPort.ToString();
                m_playerId = m_client.PlayerId;
                m_roomId = m_client.RoomId;
                m_client.OnLoginSuccess += HandleLoginSuccess;
                m_client.OnJoinRoomResp += HandleJoinRoomResp;
            }

            if (NetworkDiagnostics.Instance == null)
                gameObject.AddComponent<NetworkDiagnostics>();
            m_diagnostics = NetworkDiagnostics.Instance;

            m_runner = gameObject.AddComponent<AutoValidationRunner>();
            m_runner.OnPhaseChanged += HandlePhaseChanged;
            m_runner.OnReportReady += HandleReportReady;

            m_diagnostics?.Log(DiagnosticLevel.Info, "Validation console ready");
        }

        private void OnDestroy()
        {
            if (m_runner != null)
            {
                m_runner.OnPhaseChanged -= HandlePhaseChanged;
                m_runner.OnReportReady -= HandleReportReady;
            }
            if (m_client != null)
            {
                m_client.OnLoginSuccess -= HandleLoginSuccess;
                m_client.OnJoinRoomResp -= HandleJoinRoomResp;
            }
            if (Instance == this) Instance = null;
        }

        private void Update()
        {
            if (Input.GetKeyDown(KeyCode.F7))
            {
                m_showPanel = !m_showPanel;
            }
        }

        private void HandleLoginSuccess(LoginResp resp)
        {
            m_playerId = resp.PlayerId;
            if (m_manualJoinPending)
            {
                m_manualJoinPending = false;
                m_client?.JoinRoom(resp.PlayerId);
            }
        }

        private void HandleJoinRoomResp(JoinRoomResp resp)
        {
            if (resp.Code == 0)
            {
                m_roomId = resp.RoomId;
                m_entityId = resp.PlayerEntityId;
            }
            else if (resp.Code == -3)
            {
                m_roomId = resp.RoomId;
                if (!string.IsNullOrEmpty(resp.RedirectAddress))
                {
                    int colon = resp.RedirectAddress.LastIndexOf(':');
                    if (colon > 0)
                    {
                        m_host = resp.RedirectAddress.Substring(0, colon);
                        m_port = resp.RedirectAddress.Substring(colon + 1);
                    }
                    else
                    {
                        m_host = resp.RedirectAddress;
                    }
                }
            }
        }

        private void HandlePhaseChanged(string phase)
        {
            m_phase = phase;
        }

        private void HandleReportReady(string report)
        {
            m_report = report;
        }

        private void OnGUI()
        {
            if (!m_showPanel) return;
            EnsureStyles();

            m_client = NetworkClient.Instance ?? m_client;
            m_diagnostics = NetworkDiagnostics.Instance ?? m_diagnostics;

            int panelWidth = Mathf.Min(780, Screen.width - 16);
            int headerHeight = 260;
            int logHeight = Mathf.Max(120, Screen.height - 260);

            GUILayout.BeginArea(new Rect(8, 8, panelWidth, headerHeight), GUI.skin.box);
            DrawHeader();
            DrawStatus();
            DrawControls();
            DrawProbeStatus();
            GUILayout.EndArea();

            GUILayout.BeginArea(new Rect(8, 16 + headerHeight, panelWidth, logHeight), GUI.skin.box);
            GUILayout.Label("Protocol Log", m_titleStyle);
            m_logScroll = GUILayout.BeginScrollView(m_logScroll);
            string logText = m_diagnostics != null ? m_diagnostics.GetLogText(120) : "";
            GUILayout.Label(logText, m_monoStyle);
            GUILayout.EndScrollView();
            GUILayout.EndArea();
        }

        private void DrawHeader()
        {
            GUILayout.BeginHorizontal();
            GUILayout.Label("Realtime Combat Server Validation Console", m_titleStyle);
            GUILayout.FlexibleSpace();
            GUILayout.Label($"Phase: {m_phase}", m_monoStyle);
            GUILayout.EndHorizontal();
        }

        private void DrawStatus()
        {
            ConnectionState state = m_client?.State ?? ConnectionState.Disconnected;
            Color stateColor = state switch
            {
                ConnectionState.Authenticated => new Color(0.25f, 1f, 0.35f),
                ConnectionState.Connected => new Color(1f, 0.85f, 0.25f),
                ConnectionState.Connecting => Color.cyan,
                ConnectionState.Authenticating => Color.cyan,
                _ => new Color(1f, 0.35f, 0.3f)
            };

            GUILayout.BeginHorizontal();
            GUI.contentColor = stateColor;
            GUILayout.Label($"State: {state}", m_monoStyle, GUILayout.Width(190));
            GUI.contentColor = Color.white;
            GUILayout.Label($"Server: {m_host}:{m_port}", m_monoStyle, GUILayout.Width(150));
            GUILayout.Label($"Player: {m_playerId}", m_monoStyle, GUILayout.Width(150));
            GUILayout.Label($"Room: {m_roomId}", m_monoStyle);
            GUILayout.EndHorizontal();

            GUILayout.BeginHorizontal();
            int minRtt = m_diagnostics != null && m_diagnostics.MinRtt != int.MaxValue ? m_diagnostics.MinRtt : 0;
            GUILayout.Label($"RTT: {m_client?.Rtt ?? 0}ms avg={m_diagnostics?.AverageRtt:F1}ms min={minRtt} max={m_diagnostics?.MaxRtt}",
                m_monoStyle, GUILayout.Width(250));
            GUILayout.Label($"Frame: {m_diagnostics?.CurrentFrameRate ?? 0}fps n={m_diagnostics?.FramesReceived}",
                m_monoStyle, GUILayout.Width(180));
            GUILayout.Label($"Msg: {m_diagnostics?.MessagesIn}/{m_diagnostics?.MessagesOut}",
                m_monoStyle, GUILayout.Width(120));
            GUILayout.Label($"Bytes: {m_diagnostics?.BytesIn}/{m_diagnostics?.BytesOut}",
                m_monoStyle, GUILayout.Width(120));
            GUILayout.EndHorizontal();

            GUILayout.BeginHorizontal();
            GUILayout.Label($"Actions: sent={m_diagnostics?.ActionsSent} ok={m_diagnostics?.ActionsAccepted} reject={m_diagnostics?.ActionsRejected}",
                m_monoStyle, GUILayout.Width(250));
            GUILayout.Label($"Errors: {m_diagnostics?.ProtocolErrors}", m_monoStyle, GUILayout.Width(100));
            GUILayout.Label($"Redirect: {m_diagnostics?.RedirectCount}", m_monoStyle, GUILayout.Width(110));
            GUILayout.Label($"Reconnect: {m_diagnostics?.ReconnectCount}", m_monoStyle);
            GUILayout.EndHorizontal();
        }

        private void DrawControls()
        {
            GUILayout.BeginHorizontal();
            GUILayout.Label("Host", m_monoStyle, GUILayout.Width(34));
            m_host = GUILayout.TextField(m_host, GUILayout.Width(110));
            GUILayout.Label("Port", m_monoStyle, GUILayout.Width(32));
            m_port = GUILayout.TextField(m_port, GUILayout.Width(46));
            GUILayout.Label("Metrics", m_monoStyle, GUILayout.Width(50));
            m_metricsPort = GUILayout.TextField(m_metricsPort, GUILayout.Width(46));
            if (GUILayout.Button("Apply", GUILayout.Width(56))) ApplyConfig();
            if (GUILayout.Button(m_client != null && m_client.IsAuthenticated ? "Disconnect" : "Connect", GUILayout.Width(96)))
                ToggleConnect();
            GUILayout.EndHorizontal();

            GUILayout.BeginHorizontal();
            if (GUILayout.Button(m_runner != null && m_runner.IsRunning ? "Abort" : "Auto Validate", GUILayout.Width(116))) StartAutoValidation();
            if (GUILayout.Button("Login + Join", GUILayout.Width(104))) ManualLoginJoin();
            if (GUILayout.Button("Send Action", GUILayout.Width(96))) SendManualAction();
            if (GUILayout.Button("Probe HTTP", GUILayout.Width(92))) StartCoroutine(ProbeHttp());
            if (GUILayout.Button("Reset", GUILayout.Width(60))) ResetConsole();
            if (GUILayout.Button("Hide", GUILayout.Width(52))) m_showPanel = false;
            GUILayout.EndHorizontal();

            GUILayout.Label("Keys: WASD Move | LMB Attack | K Skill | L Ultimate | Shift Dodge | Space Jump | F8 Connect | F9 Disconnect",
                m_monoStyle);
        }

        private void DrawProbeStatus()
        {
            GUILayout.BeginHorizontal();
            GUILayout.Label($"Health: {m_httpHealth}", m_monoStyle, GUILayout.Width(270));
            GUILayout.Label($"Metrics: {m_httpMetrics}", m_monoStyle, GUILayout.Width(270));
            GUILayout.Label($"Alerts: {m_httpAlerts}", m_monoStyle);
            GUILayout.EndHorizontal();

            string reportPreview = string.IsNullOrEmpty(m_report)
                ? "Report: not generated"
                : "Report: " + Truncate(m_report.Replace("\n", " | "), 150);
            GUILayout.Label(reportPreview, m_monoStyle);
        }

        private void ApplyConfig()
        {
            if (m_client == null) return;
            int port = int.TryParse(m_port, out int p) ? p : 9090;
            int metricsPort = int.TryParse(m_metricsPort, out int mp) ? mp : 9091;
            m_client.Configure(m_host.Trim(), port, metricsPort);
            m_diagnostics?.Log(DiagnosticLevel.Info,
                $"Target configured {m_host.Trim()}:{port} metrics:{metricsPort}");
        }

        private void ToggleConnect()
        {
            if (m_client == null) return;
            if (m_client.IsAuthenticated)
                m_client.Disconnect();
            else
                m_client.Connect();
        }

        private void ManualLoginJoin()
        {
            if (m_client == null) return;
            if (m_client.IsAuthenticated)
            {
                m_client.JoinRoom(string.IsNullOrEmpty(m_playerId) ? m_client.PlayerId : m_playerId);
            }
            else
            {
                m_manualJoinPending = true;
                m_client.Login(m_playerName, 1);
            }
        }

        private void StartAutoValidation()
        {
            if (m_client == null) return;
            if (m_runner != null && m_runner.IsRunning)
            {
                m_runner.Abort();
                return;
            }
            StartCoroutine(m_runner.Run(m_client, m_playerName));
        }

        private void SendManualAction()
        {
            if (m_client == null || m_entityId < 0)
            {
                m_diagnostics?.Log(DiagnosticLevel.Warn, "Send Action ignored: player is not in a room");
                return;
            }

            long now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            var actions = new[]
            {
                new PlayerActionData { ActionType = ActionType.Move, MoveX = 1f, Timestamp = now },
                new PlayerActionData { ActionType = ActionType.Attack, Timestamp = now }
            };
            m_client.SendPlayerAction(m_entityId, ++m_manualActionFrame, actions);
            m_diagnostics?.Log(DiagnosticLevel.Info, "Manual action sent (Move + Attack)");
        }

        private void ResetConsole()
        {
            m_runner?.Abort();
            m_report = "";
            m_httpHealth = "not checked";
            m_httpMetrics = "not checked";
            m_httpAlerts = "not checked";
            m_phase = "idle";
            m_diagnostics?.Reset();
        }

        private IEnumerator ProbeHttp()
        {
            m_httpHealth = "checking...";
            m_httpMetrics = "checking...";
            m_httpAlerts = "checking...";
            int metricsPort = int.TryParse(m_metricsPort, out int mp) ? mp : 9091;
            string baseUrl = $"http://{m_host}:{metricsPort}";
            yield return ProbeEndpoint($"{baseUrl}/health", value => m_httpHealth = value);
            yield return ProbeEndpoint($"{baseUrl}/metrics", value => m_httpMetrics = value);
            yield return ProbeEndpoint($"{baseUrl}/alerts", value => m_httpAlerts = value);
        }

        private IEnumerator ProbeEndpoint(string url, Action<string> setter)
        {
            using var req = UnityWebRequest.Get(url);
            req.timeout = 3;
            yield return req.SendWebRequest();
            if (req.result == UnityWebRequest.Result.Success)
            {
                string body = req.downloadHandler.text;
                setter($"{req.responseCode} {Truncate(body, 110)}");
                m_diagnostics?.Log(DiagnosticLevel.Info, $"HTTP {req.responseCode} {url}");
            }
            else
            {
                setter($"{req.responseCode} {req.error}");
                m_diagnostics?.Log(DiagnosticLevel.Warn, $"HTTP probe failed {url}: {req.error}");
            }
        }

        private void EnsureStyles()
        {
            if (m_styleReady) return;
            m_styleReady = true;

            m_panelTexture = MakeSolidTexture(2, 2, new Color(0.08f, 0.09f, 0.12f, 0.96f));
            GUI.skin.box.normal.background = m_panelTexture;
            GUI.skin.box.normal.textColor = Color.white;
            GUI.skin.label.normal.textColor = Color.white;
            GUI.skin.textField.normal.textColor = Color.white;
            GUI.skin.textField.focused.textColor = Color.white;
            GUI.skin.button.normal.textColor = Color.white;

            m_titleStyle = new GUIStyle(GUI.skin.label)
            {
                fontSize = 15,
                fontStyle = FontStyle.Bold
            };
            m_titleStyle.normal.textColor = Color.white;

            m_monoStyle = new GUIStyle(GUI.skin.label)
            {
                fontSize = 11,
                wordWrap = true
            };
            m_monoStyle.normal.textColor = new Color(0.82f, 0.88f, 0.95f);
        }

        private static Texture2D MakeSolidTexture(int width, int height, Color color)
        {
            var tex = new Texture2D(width, height);
            var pixels = new Color[width * height];
            for (int i = 0; i < pixels.Length; i++) pixels[i] = color;
            tex.SetPixels(pixels);
            tex.Apply();
            return tex;
        }

        private static string Truncate(string value, int maxChars)
        {
            if (string.IsNullOrEmpty(value) || value.Length <= maxChars) return value;
            return value.Substring(0, maxChars) + "...";
        }
    }
}
