using UnityEngine;

namespace ActionGameDemo.Progression
{
    public class LevelSystem : MonoBehaviour
    {
        [SerializeField] private int _level = 1;
        [SerializeField] private float _currentExp = 0f;
        [SerializeField] private int _maxLevel = 99;

        [Header("经验曲线")]
        [SerializeField] private float _baseExpRequired = 100f;
        [SerializeField] private float _expGrowthFactor = 1.2f;

        public int Level => _level;
        public float CurrentExp => _currentExp;
        public int MaxLevel => _maxLevel;
        public float ExpToNextLevel => GetExpRequired(_level);
        public float ExpProgress => _currentExp / ExpToNextLevel;
        public bool IsMaxLevel => _level >= _maxLevel;

        public event System.Action<int> OnLevelUp;
        public event System.Action<float, float> OnExpChanged;

        public float GetExpRequired(int level)
        {
            return Mathf.Floor(_baseExpRequired * Mathf.Pow(_expGrowthFactor, level - 1));
        }

        public void AddExperience(float exp)
        {
            if (IsMaxLevel || exp <= 0f) return;

            _currentExp += exp;
            OnExpChanged?.Invoke(_currentExp, ExpToNextLevel);

            while (_currentExp >= ExpToNextLevel && !IsMaxLevel)
            {
                _currentExp -= ExpToNextLevel;
                LevelUp();
            }
        }

        private void LevelUp()
        {
            _level++;
            OnLevelUp?.Invoke(_level);
#if UNITY_EDITOR
            Debug.Log($"[LevelSystem] 升级到 Lv.{_level}!");
#endif
        }

        public void SetLevel(int level)
        {
            _level = Mathf.Clamp(level, 1, _maxLevel);
        }

        public void SetExp(float exp)
        {
            _currentExp = Mathf.Max(0f, exp);
        }
    }
}
