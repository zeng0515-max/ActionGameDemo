using UnityEngine;
using ActionGameDemo.Core;

namespace ActionGameDemo.Combat
{
    /// <summary>
    /// 打击反馈管理：HitStop（命中停顿）、屏幕震动、受击闪白/闪红/元素闪色。
    /// 按 1.8 规范实现分级反馈。
    /// </summary>
    public class HitFeedback : MonoBehaviour
    {
        [Header("HitStop 设置")]
        [SerializeField, Tooltip("命中停顿时的 timeScale")] private float _hitStopTimeScale = 0.1f;

        [Header("震屏设置")]
        [SerializeField, Tooltip("震屏衰减速度")] private float _shakeDecay = 5f;

        [Header("闪色设置")]
        [SerializeField, Tooltip("闪色时长（秒）")] private float _flashDuration = 0.1f;

        [Header("防重复触发")]
        [SerializeField, Tooltip("反馈锁定时长（秒）")] private float _feedbackLockDuration = 0.05f;

        private float _hitStopTimer;
        private float _shakeTimer;
        private float _currentShakeIntensity;
        private bool _isShaking;
        private float _feedbackLockUntil;
        private static MaterialPropertyBlock _mpb;

        public static Vector3 ShakeOffset { get; private set; }

        private void OnEnable()
        {
            EventManager.OnTakeDamage += OnCharacterTakeDamage;
        }

        private void OnDisable()
        {
            EventManager.OnTakeDamage -= OnCharacterTakeDamage;
            if (_hitStopTimer > 0f)
            {
                _hitStopTimer = 0f;
                if (GameManager.Instance == null || GameManager.Instance.IsPlaying)
                    Time.timeScale = 1f;
            }
            ShakeOffset = Vector3.zero;
        }

        private void Update()
        {
            UpdateHitStop();
            UpdateScreenShake();
        }

        private void OnCharacterTakeDamage(GameObject character, float damage, float currentHealth) { }

        public void TriggerHitStop(float duration)
        {
            _hitStopTimer = duration;
            Time.timeScale = _hitStopTimeScale;
        }

        public void TriggerHitStop()
        {
            TriggerHitStop(0.03f);
        }

        private void UpdateHitStop()
        {
            if (_hitStopTimer > 0f)
            {
                _hitStopTimer -= Time.unscaledDeltaTime;
                if (_hitStopTimer <= 0f)
                {
                    _hitStopTimer = 0f;
                    if (GameManager.Instance == null || GameManager.Instance.IsPlaying)
                        Time.timeScale = 1f;
                }
            }
        }

        public void TriggerScreenShake()
        {
            _shakeTimer = 0.1f;
            _currentShakeIntensity = 0.1f;
            _isShaking = true;
        }

        public void TriggerScreenShakeCustom(float intensity, float duration)
        {
            _shakeTimer = duration;
            _currentShakeIntensity = intensity;
            _isShaking = true;
        }

        private void UpdateScreenShake()
        {
            if (!_isShaking)
            {
                ShakeOffset = Vector3.zero;
                return;
            }

            if (_shakeTimer > 0f)
            {
                _shakeTimer -= Time.unscaledDeltaTime;
                _currentShakeIntensity = Mathf.Lerp(_currentShakeIntensity, 0f, _shakeDecay * Time.unscaledDeltaTime);
                ShakeOffset = Random.insideUnitSphere * _currentShakeIntensity;
            }
            else
            {
                _isShaking = false;
                ShakeOffset = Vector3.zero;
            }
        }

        public void FlashColor(Renderer renderer, Color flashColor)
        {
            if (renderer == null) return;
            StartCoroutine(FlashColorCoroutine(renderer, flashColor));
        }

        public void FlashWhite(Renderer renderer)
        {
            FlashColor(renderer, new Color(1f, 1f, 1f, 0.8f));
        }

        private System.Collections.IEnumerator FlashColorCoroutine(Renderer renderer, Color flashColor)
        {
            if (_mpb == null) _mpb = new MaterialPropertyBlock();
            renderer.GetPropertyBlock(_mpb);
            Color originalColor = renderer.sharedMaterial != null ? renderer.sharedMaterial.color : Color.white;

            float timer = 0f;
            while (timer < _flashDuration)
            {
                timer += Time.unscaledDeltaTime;
                Color lerped = Color.Lerp(flashColor, originalColor, timer / _flashDuration);
                _mpb.SetColor("_Color", lerped);
                renderer.SetPropertyBlock(_mpb);
                yield return null;
            }

            _mpb.SetColor("_Color", originalColor);
            renderer.SetPropertyBlock(_mpb);
        }

        /// <summary>
        /// 完整打击反馈：按攻击类型分级触发 HitStop + 震屏 + 闪色。
        /// attackType: 0=普攻, 1=技能, 2=大招
        /// </summary>
        public void PlayFullFeedback(GameObject target, float damage, bool isCritical,
            ElementType attackElement = ElementType.None, int attackType = 0)
        {
            if (Time.unscaledTime < _feedbackLockUntil) return;
            _feedbackLockUntil = Time.unscaledTime + _feedbackLockDuration;

            // HitStop: 普攻0.03s / 技能0.06s / 大招0.1s / 暴击额外+0.05s
            float hitStopTime = attackType switch { 0 => 0.03f, 1 => 0.06f, 2 => 0.1f, _ => 0.03f };
            if (isCritical) hitStopTime += 0.05f;
            TriggerHitStop(hitStopTime);

            // 震屏: 轻0.1/0.1s / 中0.3/0.2s / 重0.5/0.3s / 暴击升级一档
            int shakeTier = isCritical ? Mathf.Min(attackType + 1, 3) : attackType;
            float shakeIntensity = shakeTier switch { 0 => 0.1f, 1 => 0.3f, 2 => 0.5f, _ => 0.1f };
            float shakeDuration = shakeTier switch { 0 => 0.1f, 1 => 0.2f, 2 => 0.3f, _ => 0.1f };
            TriggerScreenShakeCustom(shakeIntensity, shakeDuration);

            // 闪色: 暴击闪红 / 元素闪元素色 / 普通闪白
            Renderer renderer = target.GetComponentInChildren<Renderer>();
            if (renderer != null)
            {
                Color flashColor;
                if (isCritical)
                    flashColor = new Color(1f, 0.2f, 0.2f, 0.8f);
                else if (attackElement != ElementType.None)
                    flashColor = ElementSystem.GetElementColor(attackElement) * 0.8f;
                else
                    flashColor = new Color(1f, 1f, 1f, 0.8f);
                FlashColor(renderer, flashColor);
            }
        }

        // 兼容旧调用
        public void PlayFullFeedback(GameObject target)
        {
            PlayFullFeedback(target, 0f, false, ElementType.None, 0);
        }
    }
}
