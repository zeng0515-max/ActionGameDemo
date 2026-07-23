using UnityEngine;
using UnityEngine.UI;
using ActionGameDemo.Character;
using ActionGameDemo.Core;
using ActionGameDemo.Player;

namespace ActionGameDemo.UI.HUD
{
    /// <summary>
        /// 玩家 HUD（血量、技能冷却、大招能量），订阅 EventManager 事件驱动更新。
    /// </summary>
    public class PlayerHUD : MonoBehaviour
    {
        [Header("血量显示")]
        [SerializeField, Tooltip("血量 Slider")] private Slider _healthSlider;
        [SerializeField, Tooltip("血量文字")] private Text _healthText;

        [Header("技能显示")]
        [SerializeField, Tooltip("技能冷却 Slider")] private Slider _skillCooldownSlider;
        [SerializeField, Tooltip("技能图标")] private Image _skillIcon;

        [Header("大招显示")]
        [SerializeField, Tooltip("大招能量 Slider")] private Slider _ultimateSlider;
        [SerializeField, Tooltip("大招就绪标记")] private GameObject _ultimateReadyMark;

        private PlayerController _player;

        private void OnEnable()
        {
            EventManager.OnTakeDamage += OnHealthChanged;
            EventManager.OnHeal += OnHealthChanged;
        }

        private void OnDisable()
        {
            EventManager.OnTakeDamage -= OnHealthChanged;
            EventManager.OnHeal -= OnHealthChanged;
        }

        /// <summary>绑定目标玩家</summary>
        public void Bind(PlayerController player)
        {
            _player = player;
            UpdateHealthUI();
        }

        private void OnHealthChanged(GameObject character, float amount, float currentHealth)
        {
            if (_player == null || character != _player.gameObject) return;
            UpdateHealthUI();
        }

        private void UpdateHealthUI()
        {
            if (_player == null || _player.Stats == null) return;

            if (_healthSlider != null)
                _healthSlider.value = _player.Stats.HealthPercent;

            if (_healthText != null)
                _healthText.text = $"{(int)_player.Stats.CurrentHealth}/{(int)_player.Stats.MaxHealth}";
        }

        private void Update()
        {
            if (_player == null) return;

            UpdateSkillUI();
            UpdateUltimateUI();
        }

        private void UpdateSkillUI()
        {
            if (_skillCooldownSlider != null)
            {
                _skillCooldownSlider.value = _player.SkillCooldownProgress;
            }
        }

        private void UpdateUltimateUI()
        {
            if (_ultimateSlider != null)
            {
                _ultimateSlider.value = _player.UltimateEnergyPercent;
            }

            if (_ultimateReadyMark != null)
            {
                _ultimateReadyMark.SetActive(_player.IsUltimateReady);
            }
        }
    }
}
