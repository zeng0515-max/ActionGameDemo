using UnityEngine;
using UnityEngine.UI;
using ActionGameDemo.Core;

namespace ActionGameDemo.Combat
{
    /// <summary>
        /// 伤害飘字组件：实现 IPoolable 接口，对象池复用。
    /// 显示伤害数字，向上飘动后淡出。暴击红色大字，元素攻击元素色。
    /// </summary>
    [RequireComponent(typeof(Text))]
    public class DamageNumber : MonoBehaviour, IPoolable
    {
        [Header("飘字动画")]
        [SerializeField, Tooltip("向上飘动速度")] private float _floatSpeed = 2f;
        [SerializeField, Tooltip("飘动持续时间（秒）")] private float _duration = 1f;
        [SerializeField, Tooltip("淡出开始时间比例")] private float _fadeStartRatio = 0.5f;
        [SerializeField, Tooltip("初始缩放")] private float _startScale = 0.5f;
        [SerializeField, Tooltip("目标缩放")] private float _targetScale = 1f;
        [SerializeField, Tooltip("缩放动画速度")] private float _scaleSpeed = 10f;

        [Header("颜色")]
        [SerializeField, Tooltip("普通伤害颜色")] private Color _normalColor = Color.white;
        [SerializeField, Tooltip("暴击伤害颜色")] private Color _criticalColor = new Color(1f, 0.2f, 0.2f);
        [SerializeField, Tooltip("暴击伤害字号倍数")] private int _criticalFontSizeMultiplier = 2;
        [SerializeField, Tooltip("普通伤害字号")] private int _normalFontSize = 24;

        [Header("暴击标记")]
        [SerializeField, Tooltip("暴击标记 Text（子物体）")] private Text _criticalMarkText;

        private Text _text;
        private RectTransform _rectTransform;
        private CanvasGroup _canvasGroup;
        private float _timer;
        private bool _isActive;

        private void Awake()
        {
            _text = GetComponent<Text>();
            _rectTransform = GetComponent<RectTransform>();
            _canvasGroup = GetComponent<CanvasGroup>();
            if (_canvasGroup == null)
                _canvasGroup = gameObject.AddComponent<CanvasGroup>();
        }

        /// <summary>显示伤害数字</summary>
        public void Show(float damage, bool isCritical, ElementType element, Vector3 worldPosition)
        {
            if (_text == null) return;

            _timer = 0f;
            _isActive = true;

            // 设置文本
            _text.text = Mathf.RoundToInt(damage).ToString();

            // 设置颜色
            Color color = _normalColor;
            int fontSize = _normalFontSize;

            if (isCritical)
            {
                color = _criticalColor;
                fontSize = _normalFontSize * _criticalFontSizeMultiplier;
                if (_criticalMarkText != null)
                    _criticalMarkText.gameObject.SetActive(true);
            }
            else if (element != ElementType.None)
            {
                color = ElementSystem.GetElementColor(element);
                if (_criticalMarkText != null)
                    _criticalMarkText.gameObject.SetActive(false);
            }
            else
            {
                if (_criticalMarkText != null)
                    _criticalMarkText.gameObject.SetActive(false);
            }

            _text.color = color;
            _text.fontSize = fontSize;

            // 设置位置（世界坐标转屏幕坐标）
            if (Camera.main != null)
            {
                Vector2 screenPos = Camera.main.WorldToScreenPoint(worldPosition + Vector3.up * 1.5f);
                _rectTransform.position = screenPos;
            }

            // 初始缩放
            _rectTransform.localScale = Vector3.one * _startScale;

            // 透明度
            _canvasGroup.alpha = 1f;
        }

        private void Update()
        {
            if (!_isActive) return;

            _timer += Time.deltaTime;

            // 向上飘动
            _rectTransform.anchoredPosition += Vector2.up * _floatSpeed * Time.deltaTime * 100f;

            // 缩放动画
            float currentScale = _rectTransform.localScale.x;
            currentScale = Mathf.Lerp(currentScale, _targetScale, _scaleSpeed * Time.deltaTime);
            _rectTransform.localScale = Vector3.one * currentScale;

            // 淡出
            float fadeStart = _duration * _fadeStartRatio;
            if (_timer > fadeStart)
            {
                float fadeProgress = (_timer - fadeStart) / (_duration - fadeStart);
                _canvasGroup.alpha = Mathf.Lerp(1f, 0f, fadeProgress);
            }

            // 结束
            if (_timer >= _duration)
            {
                _isActive = false;
                ReturnToPool();
            }
        }

        private void ReturnToPool()
        {
            var pool = FindFirstObjectByType<ObjectPool>();
            if (pool != null)
                pool.ReturnObject(gameObject);
            else
                gameObject.SetActive(false);
        }

        // ---------- IPoolable 实现 ----------
        public void OnGetFromPool()
        {
            _isActive = false;
            _timer = 0f;
            if (_canvasGroup != null)
                _canvasGroup.alpha = 1f;
            if (_criticalMarkText != null)
                _criticalMarkText.gameObject.SetActive(false);
        }

        public void OnReturnToPool()
        {
            _isActive = false;
            _timer = 0f;
            if (_canvasGroup != null)
                _canvasGroup.alpha = 0f;
        }
    }
}
