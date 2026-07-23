using UnityEngine;

namespace ActionGameDemo.Combat
{
    /// <summary>
        /// 连击管理：段数计数、时间窗口、取消判定。
    /// 管理连段步数（1-4 循环），与 ComboCounter 联动评分，
    /// 与 HitFeedback 联动实现连击震动梯度反馈。
    /// </summary>
    public class ComboSystem
    {
        private readonly int _maxComboStep;
        private readonly float _comboTimeout;

        private int _currentComboStep;
        private float _lastHitTime;
        private bool _isActive;

        private readonly ComboCounter _counter;

        /// <summary>当前连段步数（1-4）</summary>
        public int CurrentComboStep => _currentComboStep;

        /// <summary>是否处于连击中</summary>
        public bool IsActive => _isActive && Time.unscaledTime - _lastHitTime < _comboTimeout;

        /// <summary>连击计数器（总命中次数 + 评分）</summary>
        public ComboCounter Counter => _counter;

        /// <summary>连段步数变化事件（step）</summary>
        public System.Action<int> OnComboStepChanged;

        /// <summary>连击重置事件</summary>
        public System.Action OnComboReset;

        /// <summary>连击震动梯度事件（intensity 0-1）</summary>
        public System.Action<float> OnComboShakeRequested;

        public ComboSystem(int maxComboStep = 4, float comboTimeout = 1.0f)
        {
            _maxComboStep = maxComboStep;
            _comboTimeout = comboTimeout;
            _counter = new ComboCounter(comboTimeout);
            _currentComboStep = 0;
        }

        /// <summary>开始新连段（进入攻击状态时调用）</summary>
        public void StartCombo()
        {
            if (!IsActive)
            {
                _currentComboStep = 0;
                _isActive = true;
            }
        }

        /// <summary>
        /// 推进到下一段连招，返回新的连段步数。
        /// 更新连招窗口时间，确保连续挥击不会因超时而重置段数。
        /// UI 事件在 RegisterHit() 中统一触发，确保只有实际命中才显示连击。
        /// </summary>
        public int TryAdvanceCombo()
        {
            StartCombo();

            _currentComboStep++;
            if (_currentComboStep > _maxComboStep)
                _currentComboStep = 1;

            // 更新连招窗口时间：连续挥击时保持连招窗口活跃，防止段数被重置
            _lastHitTime = Time.unscaledTime;
            return _currentComboStep;
        }

        /// <summary>
        /// 命中注册：只有实际命中敌人才调用此方法。
        /// 同时触发连击步数变更事件和命中计数，确保 UI 仅在命中时刷新。
        /// </summary>
        public void RegisterHit()
        {
            _counter.AddHit();
            _lastHitTime = Time.unscaledTime;

            // 只有实际命中才广播连击步数变更事件（驱动 ComboDisplay UI）
            OnComboStepChanged?.Invoke(_currentComboStep);

            // 连击震动梯度：连击数越高震屏越强
            float intensity = Mathf.Clamp01((float)_counter.HitCount / 20f);
            OnComboShakeRequested?.Invoke(intensity);
        }

        /// <summary>每帧检查超时</summary>
        public void Update()
        {
            _counter.Update();

            if (_isActive && Time.unscaledTime - _lastHitTime >= _comboTimeout)
            {
                ResetCombo();
            }
        }

        /// <summary>重置连击</summary>
        public void ResetCombo()
        {
            if (_currentComboStep != 0 || _isActive)
            {
                _currentComboStep = 0;
                _isActive = false;
                _counter.Reset();
                OnComboReset?.Invoke();
            }
        }

        /// <summary>获取当前连段应播放的动画触发器名</summary>
        public string GetAnimationTriggerName(string baseName = "Attack")
        {
            int step = Mathf.Max(1, _currentComboStep);
            return $"{baseName}_{step:D2}";
        }

        /// <summary>连击超时进度（0-1，剩余时间比例）</summary>
        public float TimeoutProgress
        {
            get
            {
                if (!_isActive) return 0f;
                float elapsed = Time.unscaledTime - _lastHitTime;
                return Mathf.Clamp01(1f - elapsed / _comboTimeout);
            }
        }
    }
}
