package com.actiongame.server.benchmark;

import java.util.concurrent.atomic.AtomicLong;

public class LoadTestClientStats {
    private final String playerName;
    private final AtomicLong framesReceived = new AtomicLong();
    private final AtomicLong actionsSent = new AtomicLong();
    private final AtomicLong actionRttTotalMs = new AtomicLong();
    private final AtomicLong actionRttCount = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong bytesReceived = new AtomicLong();
    private final AtomicLong bytesSent = new AtomicLong();

    public LoadTestClientStats(String playerName) {
        this.playerName = playerName;
    }

    public void recordFrame() { framesReceived.incrementAndGet(); }
    public void recordActionSent() { actionsSent.incrementAndGet(); }
    public void recordActionRtt(long rttMs) {
        actionRttTotalMs.addAndGet(rttMs);
        actionRttCount.incrementAndGet();
    }
    public void recordError() { errors.incrementAndGet(); }
    public void recordBytesReceived(int bytes) { bytesReceived.addAndGet(bytes); }
    public void recordBytesSent(int bytes) { bytesSent.addAndGet(bytes); }

    public String getPlayerName() { return playerName; }
    public long getFramesReceived() { return framesReceived.get(); }
    public long getActionsSent() { return actionsSent.get(); }
    public long getActionRttTotalMs() { return actionRttTotalMs.get(); }
    public long getActionRttCount() { return actionRttCount.get(); }
    public long getErrors() { return errors.get(); }
    public long getBytesReceived() { return bytesReceived.get(); }
    public long getBytesSent() { return bytesSent.get(); }

    public double getAverageActionRttMs() {
        long count = actionRttCount.get();
        return count == 0 ? 0 : (double) actionRttTotalMs.get() / count;
    }
}
