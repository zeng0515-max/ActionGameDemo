package com.actiongame.server.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 服务端权威随机数工具 (架构红线: 所有战斗随机数必须在服务端生成)
 * 支持种子模式用于确定性回放
 */
public final class RandomUtil {
    private RandomUtil() {}

    public enum RandomMode {
        THREAD_LOCAL,
        SEEDED
    }

    private static volatile RandomMode mode = RandomMode.THREAD_LOCAL;
    private static long seed = System.currentTimeMillis();
    private static java.util.Random seededRng;
    private static final Object seedLock = new Object();

    /**
     * 设置随机模式 (回放时使用SEEDED模式保证确定性)
     */
    public static void setMode(RandomMode newMode, long seedValue) {
        synchronized (seedLock) {
            mode = newMode;
            seed = seedValue;
            seededRng = new java.util.Random(seed);
        }
    }

    public static void setMode(RandomMode newMode) {
        setMode(newMode, System.currentTimeMillis());
    }

    public static RandomMode getMode() {
        return mode;
    }

    public static long getSeed() {
        return seed;
    }

    /**
     * [0, 1) 随机浮点数
     */
    public static float nextFloat() {
        if (mode == RandomMode.SEEDED) {
            synchronized (seedLock) {
                return seededRng.nextFloat();
            }
        }
        return ThreadLocalRandom.current().nextFloat();
    }

    /**
     * [min, max) 随机浮点数
     */
    public static float range(float min, float max) {
        return min + nextFloat() * (max - min);
    }

    /**
     * [min, max) 随机整数
     */
    public static int range(int min, int max) {
        if (min >= max) return min;
        if (mode == RandomMode.SEEDED) {
            synchronized (seedLock) {
                return seededRng.nextInt(min, max);
            }
        }
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    /**
     * 按概率判定 (probability为true的概率, 0~1)
     */
    public static boolean chance(float probability) {
        if (probability <= 0f) return false;
        if (probability >= 1f) return true;
        return nextFloat() < probability;
    }

    /**
     * 从数组中随机选择一个
     */
    public static <T> T pick(T[] array) {
        if (array == null || array.length == 0) return null;
        return array[range(0, array.length)];
    }
}
