package com.actiongame.server.audit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 服务端运行指标统计
 * 线程安全: 所有计数器使用 LongAdder/AtomicLong
 */
public class ServerMetrics {

    // === 连接指标 ===
    private final LongAdder totalConnections = new LongAdder();
    private final LongAdder currentConnections = new LongAdder();
    private final LongAdder loginSuccess = new LongAdder();
    private final LongAdder loginFailures = new LongAdder();

    // === 战斗指标 ===
    private final LongAdder battlesStarted = new LongAdder();
    private final LongAdder battlesEnded = new LongAdder();
    private final LongAdder totalFramesExecuted = new LongAdder();

    // === 反作弊指标 ===
    private final LongAdder cheatIncidents = new LongAdder();
    private final LongAdder playersKicked = new LongAdder();
    private final LongAdder playersBanned = new LongAdder();

    // === 操作指标 ===
    private final LongAdder totalPlayerActions = new LongAdder();
    private final LongAdder totalMessagesReceived = new LongAdder();
    private final LongAdder totalMessagesSent = new LongAdder();

    // === 错误指标 ===
    private final LongAdder totalErrors = new LongAdder();
    private final Map<String, LongAdder> errorsByModule = new ConcurrentHashMap<>();

    // === 房间指标 ===
    private final AtomicLong activeRooms = new AtomicLong(0);

    // === 启动时间 ===
    private final long serverStartTime = System.currentTimeMillis();

    // === Public API ===

    public void onConnect() {
        totalConnections.increment();
        currentConnections.increment();
    }

    public void onDisconnect() {
        currentConnections.decrement();
    }

    public void onLoginSuccess() { loginSuccess.increment(); }
    public void onLoginFailure() { loginFailures.increment(); }

    public void onBattleStart() {
        battlesStarted.increment();
        activeRooms.incrementAndGet();
    }

    public void onBattleEnd(long totalFrames) {
        battlesEnded.increment();
        totalFramesExecuted.add(totalFrames);
        activeRooms.decrementAndGet();
    }

    public void onCheatIncident() { cheatIncidents.increment(); }
    public void onPlayerKicked() { playersKicked.increment(); }
    public void onPlayerBanned() { playersBanned.increment(); }

    public void onPlayerAction() { totalPlayerActions.increment(); }
    public void onMessageReceived() { totalMessagesReceived.increment(); }
    public void onMessageSent() { totalMessagesSent.increment(); }

    public void onError(String module) {
        totalErrors.increment();
        errorsByModule.computeIfAbsent(module, k -> new LongAdder()).increment();
    }

    // === Getters ===

    public long getTotalConnections() { return totalConnections.sum(); }
    public long getCurrentConnections() { return currentConnections.sum(); }
    public long getLoginSuccess() { return loginSuccess.sum(); }
    public long getLoginFailures() { return loginFailures.sum(); }
    public long getBattlesStarted() { return battlesStarted.sum(); }
    public long getBattlesEnded() { return battlesEnded.sum(); }
    public long getTotalFramesExecuted() { return totalFramesExecuted.sum(); }
    public long getCheatIncidents() { return cheatIncidents.sum(); }
    public long getPlayersKicked() { return playersKicked.sum(); }
    public long getPlayersBanned() { return playersBanned.sum(); }
    public long getTotalPlayerActions() { return totalPlayerActions.sum(); }
    public long getTotalMessagesReceived() { return totalMessagesReceived.sum(); }
    public long getTotalMessagesSent() { return totalMessagesSent.sum(); }
    public long getTotalErrors() { return totalErrors.sum(); }
    public long getActiveRooms() { return activeRooms.get(); }
    public long getServerStartTime() { return serverStartTime; }
    public Map<String, LongAdder> getErrorsByModule() { return errorsByModule; }

    /**
     * 生成摘要报告
     */
    public String getSummary() {
        long uptime = (System.currentTimeMillis() - serverStartTime) / 1000;
        return String.format(
            "Server Metrics [uptime=%ds]: conns=%d(active=%d), logins=%d(ok)/%d(fail), " +
            "battles=%d/%d(frames=%d), rooms=%d, actions=%d, " +
            "cheats=%d(kicked=%d,banned=%d), msgs_in=%d/out=%d, errors=%d",
            uptime, getTotalConnections(), getCurrentConnections(),
            getLoginSuccess(), getLoginFailures(),
            getBattlesStarted(), getBattlesEnded(), getTotalFramesExecuted(),
            getActiveRooms(), getTotalPlayerActions(),
            getCheatIncidents(), getPlayersKicked(), getPlayersBanned(),
            getTotalMessagesReceived(), getTotalMessagesSent(), getTotalErrors()
        );
    }
}
