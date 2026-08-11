package com.actiongame.server.battle.combatsystem;

/**
 * 连击评分计算 (对应Unity ComboScorer)
 * 根据连段间隔时间判定 Perfect/Excellent/Good/Miss
 */
public final class ComboScorer {
    private ComboScorer() {}

    public static final float PERFECT_THRESHOLD = 0.6f;
    public static final float EXCELLENT_THRESHOLD = 1.0f;
    public static final float GOOD_THRESHOLD = 1.5f;

    public static ComboGrade calculateGrade(float interval) {
        if (interval < PERFECT_THRESHOLD) return ComboGrade.PERFECT;
        if (interval < EXCELLENT_THRESHOLD) return ComboGrade.EXCELLENT;
        if (interval < GOOD_THRESHOLD) return ComboGrade.GOOD;
        return ComboGrade.MISS;
    }

    public static String getGradeText(ComboGrade grade) {
        return switch (grade) {
            case PERFECT -> "Perfect!";
            case EXCELLENT -> "Excellent!";
            case GOOD -> "Good";
            case MISS -> "Miss";
            case NONE -> "";
        };
    }
}
