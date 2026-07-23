using UnityEngine;

namespace ActionGameDemo.Core
{
    /// <summary>
        /// 数学工具集：提供角度转弧度、范围限制、随机数等常用数学辅助方法。
    /// 设计原则：全部为静态方法，无状态，任何脚本均可直接调用。
    /// </summary>
    public static class MathUtils
    {
        // ---------- 常量 ----------
        private const float Rad2Deg = 180f / Mathf.PI;
        private const float Deg2Rad = Mathf.PI / 180f;

        // ---------- 角度转换 ----------
        /// <summary>角度转弧度</summary>
        public static float DegreesToRadians(float degrees)
        {
            return degrees * Deg2Rad;
        }

        /// <summary>弧度转角度</summary>
        public static float RadiansToDegrees(float radians)
        {
            return radians * Rad2Deg;
        }

        // ---------- 范围限制 ----------
        /// <summary>将值限制在 [min, max] 范围内</summary>
        public static float Clamp(float value, float min, float max)
        {
            return Mathf.Clamp(value, min, max);
        }

        /// <summary>将值限制在 [0, 1] 范围内</summary>
        public static float Clamp01(float value)
        {
            return Mathf.Clamp01(value);
        }

        /// <summary>将值限制在 [min, max] 范围内（整数版）</summary>
        public static int Clamp(int value, int min, int max)
        {
            return Mathf.Clamp(value, min, max);
        }

        // ---------- 随机数 ----------
        /// <summary>返回 [minInclusive, maxExclusive) 范围内的随机整数</summary>
        public static int RandomRange(int minInclusive, int maxExclusive)
        {
            return Random.Range(minInclusive, maxExclusive);
        }

        /// <summary>返回 [minInclusive, maxInclusive] 范围内的随机浮点数</summary>
        public static float RandomRange(float minInclusive, float maxInclusive)
        {
            return Random.Range(minInclusive, maxInclusive);
        }

        /// <summary>基于权重随机选择索引（权重数组不需要归一化）</summary>
        public static int RandomWeighted(float[] weights)
        {
            if (weights == null || weights.Length == 0)
                return 0;

            float total = 0f;
            for (int i = 0; i < weights.Length; i++)
                total += Mathf.Max(0f, weights[i]);

            if (total <= 0f)
                return 0;

            float random = Random.value * total;
            float cumulative = 0f;

            for (int i = 0; i < weights.Length; i++)
            {
                cumulative += Mathf.Max(0f, weights[i]);
                if (random <= cumulative)
                    return i;
            }

            return weights.Length - 1;
        }

        // ---------- 插值 ----------
        /// <summary>线性插值（封装 Mathf.Lerp，语义更清晰）</summary>
        public static float Lerp(float a, float b, float t)
        {
            return Mathf.Lerp(a, b, Clamp01(t));
        }

        /// <summary>平滑插值（封装 Mathf.SmoothStep）</summary>
        public static float SmoothStep(float from, float to, float t)
        {
            return Mathf.SmoothStep(from, to, Clamp01(t));
        }

        // ---------- 比较 ----------
        /// <summary>判断两个浮点数是否近似相等</summary>
        public static bool Approximately(float a, float b, float tolerance = 0.0001f)
        {
            return Mathf.Abs(a - b) <= tolerance;
        }

        /// <summary>判断数值是否在范围内</summary>
        public static bool InRange(float value, float min, float max)
        {
            return value >= min && value <= max;
        }
    }
}
