package com.actiongame.server.util;

/**
 * 数学工具类 (对应Unity Mathf)
 */
public final class MathUtils {
    private MathUtils() {}

    public static final float EPSILON = 1e-6f;

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp01(float value) {
        return clamp(value, 0f, 1f);
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    public static boolean approximately(float a, float b) {
        return Math.abs(a - b) < EPSILON;
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp01(t);
    }

    public static float inverseLerp(float a, float b, float value) {
        if (approximately(a, b)) return 0f;
        return clamp01((value - a) / (b - a));
    }

    public static float repeat(float t, float length) {
        return t - (float) Math.floor(t / length) * length;
    }

    public static float moveTowards(float current, float target, float maxDelta) {
        if (Math.abs(target - current) <= maxDelta) return target;
        return current + Math.signum(target - current) * maxDelta;
    }
}
