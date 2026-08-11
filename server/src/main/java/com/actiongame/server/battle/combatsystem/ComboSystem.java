package com.actiongame.server.battle.combatsystem;

/**
 * 连击系统 (对应Unity ComboSystem + ComboCounter)
 * 管理连段步数(1-4循环)、总命中次数、时间窗口、评分
 */
public class ComboSystem {
    private final int maxComboStep;
    private final float comboTimeout;

    private int currentComboStep;
    private int hitCount;
    private long lastHitTimeMs;
    private boolean active;
    private ComboGrade lastGrade = ComboGrade.NONE;

    public ComboSystem(int maxComboStep, float comboTimeout) {
        this.maxComboStep = maxComboStep;
        this.comboTimeout = comboTimeout;
    }

    public ComboSystem() {
        this(4, 1.0f);
    }

    /**
     * 开始新连段
     */
    public void startCombo(long currentTimeMs) {
        if (!isActive(currentTimeMs)) {
            currentComboStep = 0;
            active = true;
        }
    }

    /**
     * 推进到下一段连招, 返回新的连段步数
     */
    public int tryAdvanceCombo(long currentTimeMs) {
        startCombo(currentTimeMs);
        currentComboStep++;
        if (currentComboStep > maxComboStep) currentComboStep = 1;
        lastHitTimeMs = currentTimeMs;
        return currentComboStep;
    }

    /**
     * 注册一次命中 (只有实际命中才调用)
     * 返回当前连击数
     */
    public int registerHit(long currentTimeMs) {
        if (hitCount > 0) {
            float interval = (currentTimeMs - lastHitTimeMs) / 1000f;
            lastGrade = ComboScorer.calculateGrade(interval);
            if (lastGrade == ComboGrade.MISS) {
                hitCount = 0;
                lastHitTimeMs = currentTimeMs;
                return 0;
            }
        } else {
            lastGrade = ComboGrade.NONE;
        }

        hitCount++;
        lastHitTimeMs = currentTimeMs;
        return hitCount;
    }

    /**
     * 更新 (检查超时)
     */
    public void update(long currentTimeMs) {
        if (hitCount > 0 && (currentTimeMs - lastHitTimeMs) / 1000f >= comboTimeout) {
            resetCombo();
        }
    }

    public void resetCombo() {
        currentComboStep = 0;
        hitCount = 0;
        active = false;
        lastGrade = ComboGrade.NONE;
    }

    public boolean isActive(long currentTimeMs) {
        return active && (currentTimeMs - lastHitTimeMs) / 1000f < comboTimeout;
    }

    public int getCurrentComboStep() { return currentComboStep; }
    public int getHitCount() { return hitCount; }
    public ComboGrade getLastGrade() { return lastGrade; }

    public String getAnimationTriggerName(String baseName) {
        int step = Math.max(1, currentComboStep);
        return String.format("%s_%02d", baseName, step);
    }
}
