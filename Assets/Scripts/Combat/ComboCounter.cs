using UnityEngine;

namespace ActionGameDemo.Combat
{
    /// <summary>
        /// 连击计数器和超时管理：记录连续命中次数，超时后重置。
    /// 与 ComboSystem 配合使用：ComboSystem 管理连段步数，ComboCounter 管理总命中次数和评分。
    /// </summary>
    public class ComboCounter
    {
        private int _hitCount;
        private float _lastHitTime;
        private readonly float _comboTimeout;

        private ComboGrade _lastGrade = ComboGrade.None;

        /// <summary>当前连击命中数</summary>
        public int HitCount => _hitCount;

        /// <summary>是否处于连击中</summary>
        public bool IsComboActive => _hitCount > 0 && Time.unscaledTime - _lastHitTime < _comboTimeout;

        /// <summary>上次评分</summary>
        public ComboGrade LastGrade => _lastGrade;

        /// <summary>连击变化事件（hitCount, grade）</summary>
        public System.Action<int, ComboGrade> OnComboChanged;

        /// <summary>连击重置事件</summary>
        public System.Action OnComboReset;

        public ComboCounter(float comboTimeout = 1.0f)
        {
            _comboTimeout = comboTimeout;
        }

        /// <summary>注册一次命中，返回当前连击数</summary>
        public int AddHit()
        {
            float now = Time.unscaledTime;

            if (_hitCount > 0)
            {
                float interval = now - _lastHitTime;
                _lastGrade = ComboScorer.CalculateGrade(interval);

                if (_lastGrade == ComboGrade.Miss)
                {
                    _hitCount = 0;
                    // 修复：Miss 时不 +1，直接重置为0并返回0
                    _lastHitTime = now;
                    OnComboChanged?.Invoke(0, _lastGrade);
                    return 0;
                }
            }
            else
            {
                _lastGrade = ComboGrade.None;
            }

            _hitCount++;
            _lastHitTime = now;

            OnComboChanged?.Invoke(_hitCount, _lastGrade);
            return _hitCount;
        }

        /// <summary>每帧检查超时</summary>
        public void Update()
        {
            if (_hitCount > 0 && Time.unscaledTime - _lastHitTime >= _comboTimeout)
            {
                Reset();
            }
        }

        /// <summary>重置连击</summary>
        public void Reset()
        {
            if (_hitCount > 0)
            {
                _hitCount = 0;
                _lastGrade = ComboGrade.None;
                OnComboReset?.Invoke();
            }
        }

        /// <summary>连击进度（0-1，剩余时间比例）</summary>
        public float TimeoutProgress
        {
            get
            {
                if (_hitCount == 0) return 0f;
                float elapsed = Time.unscaledTime - _lastHitTime;
                return Mathf.Clamp01(1f - elapsed / _comboTimeout);
            }
        }
    }
}
