package com.actiongame.server.replay;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 战斗录制数据 (对应文档6.2 录制格式)
 * 采用"输入流录制": 记录每帧玩家操作+随机种子, 而非全量快照
 * 序列化为紧凑二进制格式, 5分钟战斗<500KB
 */
public class BattleRecording implements Serializable {
    private String roomId;
    private long startTimeMs;
    private long totalFrames;
    private long randomSeed;
    private String configVersion;
    private List<FrameInput> frames;

    public BattleRecording() {
        this.frames = new CopyOnWriteArrayList<>();
    }

    public BattleRecording(String roomId, long startTimeMs, long randomSeed, String configVersion) {
        this.roomId = roomId;
        this.startTimeMs = startTimeMs;
        this.randomSeed = randomSeed;
        this.configVersion = configVersion;
        this.frames = new CopyOnWriteArrayList<>();
    }

    /**
     * 录制一帧的输入数据
     */
    public void addFrame(FrameInput frame) {
        frames.add(frame);
        totalFrames = Math.max(totalFrames, frame.getFrameIndex() + 1);
    }

    // === Getters/Setters ===

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public long getStartTimeMs() { return startTimeMs; }
    public void setStartTimeMs(long startTimeMs) { this.startTimeMs = startTimeMs; }
    public long getTotalFrames() { return totalFrames; }
    public void setTotalFrames(long totalFrames) { this.totalFrames = totalFrames; }
    public long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }
    public String getConfigVersion() { return configVersion; }
    public void setConfigVersion(String configVersion) { this.configVersion = configVersion; }
    public List<FrameInput> getFrames() { return frames; }
    public void setFrames(List<FrameInput> frames) { this.frames = frames; }

    /**
     * 单帧输入数据 (玩家操作 + 时间戳)
     */
    public static class FrameInput implements Serializable {
        private long frameIndex;
        private long timestampMs;
        private List<PlayerActionRecord> actions;

        public FrameInput() {
            this.actions = new ArrayList<>();
        }

        public FrameInput(long frameIndex, long timestampMs) {
            this.frameIndex = frameIndex;
            this.timestampMs = timestampMs;
            this.actions = new ArrayList<>();
        }

        public void addAction(PlayerActionRecord action) {
            actions.add(action);
        }

        public long getFrameIndex() { return frameIndex; }
        public void setFrameIndex(long frameIndex) { this.frameIndex = frameIndex; }
        public long getTimestampMs() { return timestampMs; }
        public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }
        public List<PlayerActionRecord> getActions() { return actions; }
        public void setActions(List<PlayerActionRecord> actions) { this.actions = actions; }
    }

    /**
     * 玩家操作记录
     */
    public static class PlayerActionRecord implements Serializable {
        private int entityId;
        private int actionType;
        private float moveX;
        private float moveZ;
        private int skillId;
        private int targetEntityId;

        public PlayerActionRecord() {}

        public PlayerActionRecord(int entityId, int actionType, float moveX, float moveZ,
                                   int skillId, int targetEntityId) {
            this.entityId = entityId;
            this.actionType = actionType;
            this.moveX = moveX;
            this.moveZ = moveZ;
            this.skillId = skillId;
            this.targetEntityId = targetEntityId;
        }

        public int getEntityId() { return entityId; }
        public void setEntityId(int entityId) { this.entityId = entityId; }
        public int getActionType() { return actionType; }
        public void setActionType(int actionType) { this.actionType = actionType; }
        public float getMoveX() { return moveX; }
        public void setMoveX(float moveX) { this.moveX = moveX; }
        public float getMoveZ() { return moveZ; }
        public void setMoveZ(float moveZ) { this.moveZ = moveZ; }
        public int getSkillId() { return skillId; }
        public void setSkillId(int skillId) { this.skillId = skillId; }
        public int getTargetEntityId() { return targetEntityId; }
        public void setTargetEntityId(int targetEntityId) { this.targetEntityId = targetEntityId; }
    }
}
