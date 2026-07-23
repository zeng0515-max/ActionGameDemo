using UnityEngine;
using UnityEngine.UI;
using ActionGameDemo.Combat;

namespace ActionGameDemo.UI
{
    /// <summary>
        /// 连击数和评分的 UI 显示动画。
    /// 订阅 ComboSystem 事件，连击数变化时播放缩放动画，评分文字弹出。
    /// </summary>
    public class ComboDisplay : MonoBehaviour
    {
        [Header("连击数显示")]
        [SerializeField, Tooltip("连击数 Text")] private Text _comboText;
        [SerializeField, Tooltip("连击数前缀文本")] private string _comboPrefix = "Combo ";
        [SerializeField, Tooltip("连击数缩放动画速度")] private float _scaleAnimSpeed = 10f;
        [SerializeField, Tooltip("连击数最大缩放")] private float _maxScale = 1.3f;

        [Header("评分显示")]
        [SerializeField, Tooltip("评分文字 Text")] private Text _gradeText;
        [SerializeField, Tooltip("评分显示时长（秒）")] private float _gradeDisplayDuration = 1f;
        [SerializeField, Tooltip("Perfect 评分颜色")] private Color _perfectColor = new Color(1f, 0.84f, 0f);
        [SerializeField, Tooltip("Excellent 评分颜色")] private Color _excellentColor = new Color(0.2f, 0.6f, 1f);
        [SerializeField, Tooltip("Good 评分颜色")] private Color _goodColor = new Color(0.3f, 0.8f, 0.3f);

        [Header("超时进度条")]
        [SerializeField, Tooltip("连击超时进度条 Slider")] private Slider _timeoutSlider;

        [Header("自动隐藏")]
        [SerializeField, Tooltip("无连击时是否隐藏")] private bool _autoHide = true;
        [SerializeField, Tooltip("隐藏延迟（秒）")] private float _hideDelay = 0.5f;

        private ComboSystem _comboSystem;
        private Vector3 _targetComboScale = Vector3.one;
        private float _gradeTimer;
        private float _hideTimer;
        private bool _isVisible;

        private void OnEnable()
        {
            // 延迟查找 PlayerController（可能在 Start 之后才可用）
            Invoke(nameof(BindComboSystem), 0.1f);
        }

        private void BindComboSystem()
        {
            var player = FindFirstObjectByType<Player.PlayerController>();
            if (player != null && player.ComboSystem != null)
            {
                _comboSystem = player.ComboSystem;
                _comboSystem.OnComboStepChanged += OnComboStepChanged;
                _comboSystem.OnComboReset += OnComboReset;
                _comboSystem.Counter.OnComboChanged += OnComboChanged;
                _comboSystem.Counter.OnComboReset += OnComboCounterReset;
            }
            else
            {
                // 重试
                Invoke(nameof(BindComboSystem), 0.5f);
            }
        }

        private void OnDisable()
        {
            CancelInvoke(nameof(BindComboSystem));

            if (_comboSystem != null)
            {
                _comboSystem.OnComboStepChanged -= OnComboStepChanged;
                _comboSystem.OnComboReset -= OnComboReset;
                _comboSystem.Counter.OnComboChanged -= OnComboChanged;
                _comboSystem.Counter.OnComboReset -= OnComboCounterReset;
            }
        }

        private void Update()
        {
            UpdateComboScale();
            UpdateGradeText();
            UpdateTimeoutSlider();
            UpdateAutoHide();
        }

        private void OnComboStepChanged(int step)
        {
            if (_comboText != null)
            {
                _comboText.text = $"{_comboPrefix}{step}";
                _targetComboScale = Vector3.one * _maxScale;
            }
            Show();
        }

        private void OnComboReset()
        {
            _hideTimer = _hideDelay;
        }

        private void OnComboChanged(int hitCount, ComboGrade grade)
        {
            if (_comboText != null)
            {
                _comboText.text = $"{_comboPrefix}{hitCount}";
                _targetComboScale = Vector3.one * _maxScale;
            }

            if (_gradeText != null && grade != ComboGrade.None)
            {
                _gradeText.text = ComboScorer.GetGradeText(grade);
                _gradeText.color = grade switch
                {
                    ComboGrade.Perfect => _perfectColor,
                    ComboGrade.Excellent => _excellentColor,
                    ComboGrade.Good => _goodColor,
                    _ => Color.white
                };
                _gradeTimer = _gradeDisplayDuration;
                _gradeText.gameObject.SetActive(true);
            }

            Show();
        }

        private void OnComboCounterReset()
        {
            _hideTimer = _hideDelay;
        }

        private void UpdateComboScale()
        {
            if (_comboText == null) return;

            _targetComboScale = Vector3.Lerp(_targetComboScale, Vector3.one, _scaleAnimSpeed * Time.deltaTime);
            _comboText.rectTransform.localScale = _targetComboScale;
        }

        private void UpdateGradeText()
        {
            if (_gradeText == null || !_gradeText.gameObject.activeInHierarchy) return;

            if (_gradeTimer > 0f)
            {
                _gradeTimer -= Time.deltaTime;
                if (_gradeTimer <= 0f)
                {
                    _gradeText.gameObject.SetActive(false);
                }
            }
        }

        private void UpdateTimeoutSlider()
        {
            if (_timeoutSlider == null || _comboSystem == null) return;

            _timeoutSlider.value = _comboSystem.TimeoutProgress;
        }

        private void UpdateAutoHide()
        {
            if (!_autoHide) return;

            if (_comboSystem == null || !_comboSystem.IsActive)
            {
                if (_isVisible)
                {
                    _hideTimer -= Time.deltaTime;
                    if (_hideTimer <= 0f)
                    {
                        Hide();
                    }
                }
            }
        }

        private void Show()
        {
            _isVisible = true;
            if (_comboText != null) _comboText.gameObject.SetActive(true);
            if (_timeoutSlider != null) _timeoutSlider.gameObject.SetActive(true);
        }

        private void Hide()
        {
            _isVisible = false;
            if (_comboText != null) _comboText.gameObject.SetActive(false);
            if (_gradeText != null) _gradeText.gameObject.SetActive(false);
            if (_timeoutSlider != null) _timeoutSlider.gameObject.SetActive(false);
        }
    }
}
