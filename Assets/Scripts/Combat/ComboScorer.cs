namespace ActionGameDemo.Combat
{
    /// <summary>
        /// 连击评分计算：根据连段间隔时间判定 Perfect/Excellent/Good/Miss。
    /// 评分阈值可配置，支持外部查询当前评级。
    /// </summary>
    public enum ComboGrade
    {
        None,
        Perfect,
        Excellent,
        Good,
        Miss
    }

    public static class ComboScorer
    {
        // 评分阈值（秒）：间隔时间 < 阈值 → 对应评级
        public const float PERFECT_THRESHOLD = 0.6f;
        public const float EXCELLENT_THRESHOLD = 1.0f;
        public const float GOOD_THRESHOLD = 1.5f;
        // > GOOD_THRESHOLD → Miss

        /// <summary>根据连段间隔时间计算评级</summary>
        public static ComboGrade CalculateGrade(float interval)
        {
            if (interval < PERFECT_THRESHOLD)
                return ComboGrade.Perfect;
            if (interval < EXCELLENT_THRESHOLD)
                return ComboGrade.Excellent;
            if (interval < GOOD_THRESHOLD)
                return ComboGrade.Good;
            return ComboGrade.Miss;
        }

        /// <summary>评级对应的显示文本</summary>
        public static string GetGradeText(ComboGrade grade)
        {
            return grade switch
            {
                ComboGrade.Perfect => "Perfect!",
                ComboGrade.Excellent => "Excellent!",
                ComboGrade.Good => "Good",
                ComboGrade.Miss => "Miss",
                _ => ""
            };
        }
    }
}
