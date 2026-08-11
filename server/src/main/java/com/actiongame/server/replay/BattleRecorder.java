package com.actiongame.server.replay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 帧录制器 (对应文档6.2 BattleRecorder)
 *
 * 战斗开始时创建, 记录初始快照/配置版本/随机种子
 * 每帧记录该帧所有玩家操作和帧索引
 * 战斗结束时序列化为紧凑二进制格式存储
 */
public class BattleRecorder {
    private static final Logger log = LoggerFactory.getLogger(BattleRecorder.class);

    private final BattleRecording recording;
    private volatile boolean active = false;

    public BattleRecorder(String roomId, long randomSeed, String configVersion) {
        this.recording = new BattleRecording(roomId, System.currentTimeMillis(), randomSeed, configVersion);
    }

    /**
     * 开始录制
     */
    public void start() {
        active = true;
        log.info("Battle recording started: roomId={}, seed={}", recording.getRoomId(), recording.getRandomSeed());
    }

    /**
     * 录制一帧玩家操作
     */
    public void recordFrame(long frameIndex, long timestampMs) {
        if (!active) return;

        BattleRecording.FrameInput frame = new BattleRecording.FrameInput(frameIndex, timestampMs);
        recording.addFrame(frame);
    }

    /**
     * 录制一帧玩家操作 (带action)
     */
    public void recordFrame(long frameIndex, long timestampMs, BattleRecording.PlayerActionRecord action) {
        if (!active) return;

        BattleRecording.FrameInput frame = findOrCreateFrame(frameIndex, timestampMs);
        frame.addAction(action);
    }

    private BattleRecording.FrameInput findOrCreateFrame(long frameIndex, long timestampMs) {
        var frames = recording.getFrames();
        if (!frames.isEmpty()) {
            var last = frames.get(frames.size() - 1);
            if (last.getFrameIndex() == frameIndex) return last;
        }

        BattleRecording.FrameInput frame = new BattleRecording.FrameInput(frameIndex, timestampMs);
        recording.addFrame(frame);
        return frame;
    }

    /**
     * 停止录制, 返回录制数据
     */
    public BattleRecording stop() {
        active = false;
        recording.setTotalFrames(recording.getFrames().size());
        log.info("Battle recording stopped: roomId={}, frames={}",
            recording.getRoomId(), recording.getTotalFrames());
        return recording;
    }

    public boolean isRecording() { return active; }
    public BattleRecording getRecording() { return recording; }
    public String getRoomId() { return recording.getRoomId(); }
    public long getRandomSeed() { return recording.getRandomSeed(); }
    public long getTotalFrames() { return recording.getTotalFrames(); }
}
