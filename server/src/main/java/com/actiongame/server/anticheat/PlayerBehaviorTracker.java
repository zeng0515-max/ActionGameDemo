package com.actiongame.server.anticheat;

import com.actiongame.server.util.Vector3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 玩家行为追踪器 (滑动窗口)
 * 记录最近N帧的移动距离和攻击时间戳, 用于实时检测
 */
public class PlayerBehaviorTracker {

    private final int entityId;
    private final Deque<MoveRecord> moveRecords;
    private final Deque<Long> attackTimestamps;
    private long lastDodgeTimestamp;
    private Vector3 lastPosition;
    private long lastFrameIndex;

    /** 最大移动速度 (用于检测加速) */
    private float maxObservedSpeed = 0f;

    private static final int MAX_MOVE_RECORDS = AntiCheatConfig.TRACKER_WINDOW_FRAMES;
    private static final int MAX_ATTACK_RECORDS = 100;

    public PlayerBehaviorTracker(int entityId) {
        this.entityId = entityId;
        this.moveRecords = new ArrayDeque<>();
        this.attackTimestamps = new ArrayDeque<>();
        this.lastDodgeTimestamp = 0;
    }

    /**
     * 记录一次移动
     * @param frameIndex 帧索引
     * @param newPosition 新位置
     * @param deltaTime 时间增量 (秒)
     */
    public void recordMove(long frameIndex, Vector3 newPosition, float deltaTime) {
        if (lastPosition != null && deltaTime > 0f) {
            float distance = newPosition.subtract(lastPosition).magnitude();
            float speed = distance / deltaTime;
            if (speed > maxObservedSpeed) {
                maxObservedSpeed = speed;
            }
            moveRecords.addLast(new MoveRecord(frameIndex, distance, speed));
            while (moveRecords.size() > MAX_MOVE_RECORDS) {
                moveRecords.removeFirst();
            }
        }
        lastPosition = newPosition;
        lastFrameIndex = frameIndex;
    }

    /**
     * 记录一次攻击
     * @param timestampMs 时间戳 (毫秒)
     */
    public void recordAttack(long timestampMs) {
        attackTimestamps.addLast(timestampMs);
        while (attackTimestamps.size() > MAX_ATTACK_RECORDS) {
            attackTimestamps.removeFirst();
        }
    }

    /**
     * 获取最近1秒内的攻击次数
     */
    public int getAttackCountInLastSecond(long currentTimestampMs) {
        long cutoff = currentTimestampMs - 1000;
        int count = 0;
        for (long ts : attackTimestamps) {
            if (ts >= cutoff) count++;
        }
        return count;
    }

    /**
     * 获取最近一次攻击时间戳
     */
    public long getLastAttackTimestamp() {
        return attackTimestamps.isEmpty() ? 0 : attackTimestamps.peekLast();
    }

    /**
     * 记录一次闪避
     */
    public void recordDodge(long timestampMs) {
        lastDodgeTimestamp = timestampMs;
    }

    /**
     * 获取最近一次闪避时间戳
     */
    public long getLastDodgeTimestamp() {
        return lastDodgeTimestamp;
    }

    /**
     * 获取最近N帧的累计移动距离
     */
    public float getRecentMoveDistance(int frames) {
        float total = 0f;
        int count = 0;
        for (var it = moveRecords.descendingIterator(); it.hasNext() && count < frames; ) {
            total += it.next().distance;
            count++;
        }
        return total;
    }

    /**
     * 获取最近N帧的平均移动速度
     */
    public float getRecentAvgSpeed(int frames, float deltaTime) {
        if (deltaTime <= 0 || frames <= 0) return 0f;
        return getRecentMoveDistance(frames) / (frames * deltaTime);
    }

    public float getMaxObservedSpeed() { return maxObservedSpeed; }
    public Vector3 getLastPosition() { return lastPosition; }
    public long getLastFrameIndex() { return lastFrameIndex; }
    public int getEntityId() { return entityId; }

    /**
     * 重置 (玩家重连或死亡复活)
     */
    public void reset() {
        moveRecords.clear();
        attackTimestamps.clear();
        lastPosition = null;
        lastFrameIndex = 0;
        maxObservedSpeed = 0f;
        lastDodgeTimestamp = 0;
    }

    private record MoveRecord(long frameIndex, float distance, float speed) {}
}
