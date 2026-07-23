using UnityEngine;
using UnityEngine.UI;
using ActionGameDemo.Character;

namespace ActionGameDemo.UI
{
    public class EnemyHealthBar : MonoBehaviour
    {
        [SerializeField] private Slider _healthSlider;
        [SerializeField] private CanvasGroup _canvasGroup;
        [SerializeField] private float _hideDelay = 5f;
        [SerializeField] private bool _alwaysVisible = true;
        [SerializeField] private Vector3 _offset = new Vector3(0f, 2.5f, 0f);

        private CharacterBase _target;
        private float _lastDamageTime = -999f;

        private void Start()
        {
            _target = GetComponentInParent<CharacterBase>();
            if (_target != null && _target.Stats != null)
            {
                _target.Stats.OnHealthChanged += OnHealthChanged;
                if (_healthSlider != null)
                    _healthSlider.value = _target.Stats.HealthPercent;
            }
            if (_canvasGroup == null) _canvasGroup = GetComponent<CanvasGroup>();
            if (_canvasGroup != null) _canvasGroup.alpha = 1f;
        }

        private void OnDestroy()
        {
            if (_target != null && _target.Stats != null)
                _target.Stats.OnHealthChanged -= OnHealthChanged;
        }

        private void LateUpdate()
        {
            if (_target == null) return;
            transform.position = _target.transform.position + _offset;
            transform.LookAt(Camera.main != null ? Camera.main.transform : transform);
            transform.eulerAngles = new Vector3(0, transform.eulerAngles.y, 0);

            if (_canvasGroup != null && !_alwaysVisible)
            {
                bool show = Time.time - _lastDamageTime < _hideDelay && !_target.IsDead;
                _canvasGroup.alpha = Mathf.Lerp(_canvasGroup.alpha, show ? 1f : 0f, 10f * Time.deltaTime);
            }
        }

        private void OnHealthChanged(float current, float max)
        {
            if (_healthSlider != null)
                _healthSlider.value = max > 0f ? current / max : 0f;
            _lastDamageTime = Time.time;
        }
    }
}
