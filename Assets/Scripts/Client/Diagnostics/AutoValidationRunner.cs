using System;
using System.Collections;
using System.IO;
using System.Text;
using ActionGameDemo.Protocol;
using UnityEngine;
using UnityEngine.Networking;

namespace ActionGameDemo.Client.Diagnostics
{
    /// <summary>
    /// End-to-end validation flow: connect -> login -> join room -> battle start ->
    /// player action round trip -> frame stream -> HTTP observability endpoints.
    /// </summary>
    public sealed class AutoValidationRunner : MonoBehaviour
    {
        public event Action<string> OnPhaseChanged;
        public event Action<string> OnReportReady;

        public bool IsRunning { get; private set; }
        public string CurrentPhase { get; private set; } = "idle";

        private NetworkClient m_client;
        private NetworkDiagnostics m_diagnostics;
        private readonly StringBuilder m_checks = new();
        private readonly StringBuilder m_report = new();
        private bool m_abort;
        private bool m_failed;
        private bool m_loginDone;
        private bool m_joinDone;
        private bool m_battleStarted;
        private string m_playerId = "";
        private int m_entityId = -1;
        private string m_roomId = "";
        private long m_actionFrameIndex;

        public IEnumerator Run(NetworkClient client, string playerName)
        {
            if (IsRunning) yield break;
            IsRunning = true;
            m_abort = false;
            m_failed = false;
            m_client = client;
            m_diagnostics = NetworkDiagnostics.Instance;
            m_playerId = "";
            m_entityId = -1;
            m_roomId = "";
            m_loginDone = false;
            m_joinDone = false;
            m_battleStarted = false;
            m_actionFrameIndex = 0;
            m_checks.Clear();
            m_report.Clear();

            if (m_client == null)
            {
                Fail("setup", "NetworkClient is missing");
                Finish();
                yield break;
            }

            Subscribe();
            try
            {
                yield return StartConnectAndAuthenticate(playerName);
                if (m_failed) { Finish(); yield break; }
                AddCheck("connect+login", true, $"player={m_playerId}");

                yield return EnsureJoined();
                if (m_failed) { Finish(); yield break; }
                AddCheck("join-room", true, $"room={m_roomId} entity={m_entityId}");

                yield return WaitBattleStart();
                if (m_failed) { Finish(); yield break; }
                AddCheck("battle-start", true, $"room={m_roomId}");

                yield return SendActionBursts();
                if (m_failed) { Finish(); yield break; }
                AddCheck("action-pipeline", true,
                    $"sent={m_diagnostics?.ActionsSent} accepted={m_diagnostics?.ActionsAccepted}");

                yield return MeasureFrameStream();
                if (m_failed) { Finish(); yield break; }
                AddCheck("frame-stream", true,
                    $"frames={m_diagnostics?.FramesReceived} fps={m_diagnostics?.CurrentFrameRate}");

                yield return ProbeMetrics();
            }
            finally
            {
                Unsubscribe();
            }

            Finish();
        }

        public void Abort()
        {
            m_abort = true;
        }

        private void Subscribe()
        {
            m_client.OnLoginSuccess += HandleLoginSuccess;
            m_client.OnJoinRoomResp += HandleJoinRoomResp;
            m_client.OnBattleStart += HandleBattleStart;
        }

        private void Unsubscribe()
        {
            if (m_client == null) return;
            m_client.OnLoginSuccess -= HandleLoginSuccess;
            m_client.OnJoinRoomResp -= HandleJoinRoomResp;
            m_client.OnBattleStart -= HandleBattleStart;
        }

        private void HandleLoginSuccess(LoginResp resp)
        {
            m_loginDone = true;
            m_playerId = resp.PlayerId;
        }

        private void HandleJoinRoomResp(JoinRoomResp resp)
        {
            if (resp.Code == 0)
            {
                m_joinDone = true;
                m_entityId = resp.PlayerEntityId;
                m_roomId = resp.RoomId;
            }
            else if (resp.Code == -3)
            {
                m_roomId = resp.RoomId;
            }
        }

        private void HandleBattleStart(BattleStartNotify notify)
        {
            m_battleStarted = true;
            m_roomId = notify.RoomId;
        }

        private IEnumerator StartConnectAndAuthenticate(string playerName)
        {
            SetPhase("connect");

            if (!m_client.IsAuthenticated)
            {
                m_client.Connect();
                float wait = 0f;
                while (!m_client.IsAuthenticated && wait < 8f && !m_abort)
                {
                    yield return null;
                    wait += Time.unscaledDeltaTime;
                }
            }

            if (!m_client.IsAuthenticated && !m_loginDone)
            {
                m_client.Login(playerName, 1);
                float wait = 0f;
                while (!m_loginDone && !m_client.IsAuthenticated && wait < 5f && !m_abort)
                {
                    yield return null;
                    wait += Time.unscaledDeltaTime;
                }
            }

            if (m_abort)
            {
                Fail("connect+login", "aborted by user");
                yield break;
            }
            if (!m_client.IsAuthenticated)
            {
                Fail("connect+login", "login timeout or rejected");
                yield break;
            }

            if (string.IsNullOrEmpty(m_playerId))
                m_playerId = m_client.PlayerId;
            m_diagnostics?.Log(DiagnosticLevel.Info, $"Validation connect+login OK player={m_playerId}");
        }

        private IEnumerator EnsureJoined()
        {
            SetPhase("join-room");

            if (string.IsNullOrEmpty(m_roomId) && string.IsNullOrEmpty(m_client.RoomId))
            {
                m_client.JoinRoom(string.IsNullOrEmpty(m_playerId) ? m_client.PlayerId : m_playerId);
                float wait = 0f;
                while (!m_joinDone && wait < 8f && !m_abort)
                {
                    yield return null;
                    wait += Time.unscaledDeltaTime;
                }
            }

            if (m_abort)
            {
                Fail("join-room", "aborted by user");
                yield break;
            }
            if (!m_joinDone && m_entityId < 0)
            {
                Fail("join-room", "JoinRoomResp not received with code=0");
                yield break;
            }

            m_diagnostics?.Log(DiagnosticLevel.Info, $"Validation joined room={m_roomId} entity={m_entityId}");
        }

        private IEnumerator WaitBattleStart()
        {
            SetPhase("battle-start");
            float wait = 0f;
            while (!m_battleStarted && wait < 6f && !m_abort)
            {
                yield return null;
                wait += Time.unscaledDeltaTime;
                if (m_diagnostics != null && m_diagnostics.FramesReceived > 0) break;
            }

            if (m_abort)
            {
                Fail("battle-start", "aborted by user");
                yield break;
            }
            if (!m_battleStarted && (m_diagnostics == null || m_diagnostics.FramesReceived == 0))
            {
                Fail("battle-start", "BattleStartNotify not received and no frames arrived");
                yield break;
            }

            m_diagnostics?.Log(DiagnosticLevel.Info, "Validation battle started");
        }

        private IEnumerator SendActionBursts()
        {
            SetPhase("action-pipeline");
            if (m_entityId < 0)
            {
                Fail("action-pipeline", "player entity id is missing");
                yield break;
            }

            for (int i = 0; i < 8 && !m_abort; i++)
            {
                long now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                var actions = new[]
                {
                    new PlayerActionData { ActionType = ActionType.Move, MoveX = 1f, Timestamp = now },
                    new PlayerActionData { ActionType = ActionType.Attack, Timestamp = now }
                };
                m_client.SendPlayerAction(m_entityId, ++m_actionFrameIndex, actions);
                yield return WaitRealtime(0.4f);
            }

            float wait = 0f;
            while (m_diagnostics != null && m_diagnostics.ActionsAccepted == 0 && wait < 2f && !m_abort)
            {
                yield return null;
                wait += Time.unscaledDeltaTime;
            }

            if (m_abort)
            {
                Fail("action-pipeline", "aborted by user");
                yield break;
            }
            if (m_diagnostics == null || m_diagnostics.ActionsAccepted == 0)
            {
                Fail("action-pipeline", "no PlayerActionResp accepted");
                yield break;
            }

            m_diagnostics?.Log(DiagnosticLevel.Info,
                $"Validation action pipeline OK sent={m_diagnostics?.ActionsSent} accepted={m_diagnostics?.ActionsAccepted}");
        }

        private IEnumerator MeasureFrameStream()
        {
            SetPhase("frame-stream");
            long startFrames = m_diagnostics?.FramesReceived ?? 0;
            yield return WaitRealtime(3f);
            long delta = (m_diagnostics?.FramesReceived ?? 0) - startFrames;
            int fps = Mathf.RoundToInt(delta / 3f);

            if (fps < 30)
            {
                Fail("frame-stream", $"frame stream too slow: {delta} frames in 3s ({fps} fps)");
                yield break;
            }

            m_diagnostics?.Log(DiagnosticLevel.Info, $"Validation frame stream OK {delta} frames in 3s ({fps} fps)");
        }

        private IEnumerator ProbeMetrics()
        {
            SetPhase("metrics");
            string baseUrl = $"http://{m_client.ServerHost}:{m_client.MetricsPort}";
            yield return ProbeAndRecord($"{baseUrl}/health", "health");
            yield return ProbeAndRecord($"{baseUrl}/metrics", "metrics");
            yield return ProbeAndRecord($"{baseUrl}/alerts", "alerts");
        }

        private IEnumerator ProbeAndRecord(string url, string label)
        {
            bool ok = false;
            string detail;
            using (var req = UnityWebRequest.Get(url))
            {
                req.timeout = 3;
                yield return req.SendWebRequest();
                if (req.result == UnityWebRequest.Result.Success && req.responseCode == 200)
                {
                    string body = req.downloadHandler.text;
                    detail = $"HTTP 200, {body.Length} bytes";
                    if (label == "health" && body.Contains("\"ok\""))
                        ok = true;
                    else if (label == "metrics" && body.Contains("actiongame_messages_received_total"))
                        ok = true;
                    else if (label == "alerts")
                        ok = true;
                }
                else
                {
                    detail = $"HTTP {req.responseCode} {req.error}";
                }
            }

            AddCheck(label, ok, detail);
            if (!ok) m_failed = true;
            m_diagnostics?.Log(ok ? DiagnosticLevel.Info : DiagnosticLevel.Warn,
                $"HTTP {label} probe: {detail}");
        }

        private void Finish()
        {
            bool pass = !m_failed;
            BuildReport(pass);
            IsRunning = false;
        }

        private void BuildReport(bool pass)
        {
            m_report.Clear();
            m_report.AppendLine("=== Realtime Combat Server Validation Report ===");
            m_report.AppendLine($"Timestamp: {DateTime.Now:yyyy-MM-dd HH:mm:ss}");
            m_report.AppendLine($"Target: {m_client?.ServerHost}:{m_client?.ServerPort} metrics:{m_client?.MetricsPort}");
            m_report.AppendLine($"Result: {(pass ? "PASS" : "FAIL")}");
            m_report.AppendLine($"LastPhase: {CurrentPhase}");
            m_report.AppendLine($"PlayerId: {m_playerId}");
            m_report.AppendLine($"RoomId: {m_roomId}");
            m_report.AppendLine($"EntityId: {m_entityId}");
            int minRtt = m_diagnostics != null && m_diagnostics.MinRtt != int.MaxValue
                ? m_diagnostics.MinRtt
                : 0;
            m_report.AppendLine($"RTT: avg={m_diagnostics?.AverageRtt:F1}ms min={minRtt} max={m_diagnostics?.MaxRtt}");
            m_report.AppendLine($"FrameRate: {m_diagnostics?.CurrentFrameRate} fps");
            m_report.AppendLine($"Frames: {m_diagnostics?.FramesReceived}");
            m_report.AppendLine($"Messages: in={m_diagnostics?.MessagesIn} out={m_diagnostics?.MessagesOut}");
            m_report.AppendLine($"Bytes: in={m_diagnostics?.BytesIn} out={m_diagnostics?.BytesOut}");
            m_report.AppendLine($"Actions: sent={m_diagnostics?.ActionsSent} accepted={m_diagnostics?.ActionsAccepted} rejected={m_diagnostics?.ActionsRejected}");
            m_report.AppendLine($"Errors: protocol={m_diagnostics?.ProtocolErrors} redirects={m_diagnostics?.RedirectCount}");
            m_report.AppendLine("Checks:");
            m_report.Append(m_checks);

            string text = m_report.ToString();
            string path = Path.Combine(Application.persistentDataPath,
                $"validation-report-{DateTime.Now:yyyyMMdd-HHmmss}.txt");
            try
            {
                File.WriteAllText(path, text);
                m_report.AppendLine();
                m_report.AppendLine($"Report file: {path}");
                m_diagnostics?.Log(DiagnosticLevel.Info, $"Validation {GetResultText(pass)} report written to {path}");
            }
            catch (Exception ex)
            {
                m_diagnostics?.Log(DiagnosticLevel.Error, "Failed to write validation report: " + ex.Message);
            }

            Debug.Log(text);
            OnReportReady?.Invoke(text);
        }

        private static string GetResultText(bool pass)
        {
            return pass ? "PASS" : "FAIL";
        }

        private void AddCheck(string name, bool ok, string detail)
        {
            m_checks.AppendLine($"  [{(ok ? "PASS" : "FAIL")}] {name}: {detail}");
        }

        private void Fail(string step, string reason)
        {
            m_failed = true;
            AddCheck(step, false, reason);
            m_diagnostics?.Log(DiagnosticLevel.Error, $"Validation {step} failed: {reason}");
        }

        private void SetPhase(string phase)
        {
            CurrentPhase = phase;
            OnPhaseChanged?.Invoke(phase);
        }

        private static IEnumerator WaitRealtime(float seconds)
        {
            float end = Time.realtimeSinceStartup + seconds;
            while (Time.realtimeSinceStartup < end) yield return null;
        }
    }
}
