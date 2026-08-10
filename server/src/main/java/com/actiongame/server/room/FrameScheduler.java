package com.actiongame.server.room;

import com.actiongame.server.constant.GameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;

/**
 * 帧调度器 (对应文档3.5 FrameScheduler)
 * 固定时间步长 (20ms/帧 = 50fps), 每帧调用FrameExecutor
 */
public class FrameScheduler {
    private static final Logger log = LoggerFactory.getLogger(FrameScheduler.class);

    private final long frameIntervalMs;
    private final FrameExecutor executor;
    private Thread schedulerThread;
    private volatile boolean running = false;

    public FrameScheduler(FrameExecutor executor) {
        this(executor, GameConstants.FRAME_INTERVAL_MS);
    }

    public FrameScheduler(FrameExecutor executor, long frameIntervalMs) {
        this.executor = executor;
        this.frameIntervalMs = frameIntervalMs;
    }

    /**
     * 启动帧调度
     */
    public void start() {
        if (running) return;
        running = true;
        schedulerThread = new Thread(this::runLoop, "FrameScheduler");
        schedulerThread.setDaemon(true);
        schedulerThread.start();
        log.info("FrameScheduler started, interval={}ms", frameIntervalMs);
    }

    /**
     * 停止帧调度
     */
    public void stop() {
        running = false;
        if (schedulerThread != null) {
            schedulerThread.interrupt();
            if (schedulerThread != Thread.currentThread()) {
                try {
                    schedulerThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        log.info("FrameScheduler stopped");
    }

    private void runLoop() {
        long frameIndex = 0;
        long nextFrameTime = System.nanoTime();
        final long intervalNanos = frameIntervalMs * 1_000_000L;
        final int MAX_CATCHUP_FRAMES = 5;

        while (running) {
            long currentTime = System.nanoTime();

            if (currentTime < nextFrameTime) {
                long sleepNanos = nextFrameTime - currentTime;
                if (sleepNanos > 0) {
                    try {
                        LockSupport.parkNanos(sleepNanos);
                    } catch (Exception e) {
                        if (Thread.currentThread().isInterrupted()) break;
                    }
                }
                continue;
            }

            float deltaTime = frameIntervalMs / 1000f;
            try {
                executor.executeFrame(frameIndex, deltaTime);
            } catch (Exception e) {
                log.error("Error executing frame {}", frameIndex, e);
            }

            frameIndex++;
            nextFrameTime += intervalNanos;

            long now = System.nanoTime();
            long deficit = (now - nextFrameTime) / intervalNanos;
            if (deficit > MAX_CATCHUP_FRAMES) {
                log.warn("Frame scheduler {} frames behind, resetting timer", deficit);
                nextFrameTime = now;
            }
        }
    }

    public boolean isRunning() { return running; }
}
