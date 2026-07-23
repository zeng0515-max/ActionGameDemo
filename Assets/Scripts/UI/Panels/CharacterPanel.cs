using UnityEngine;
using UnityEngine.UI;
using ActionGameDemo.Character;
using ActionGameDemo.Progression;

namespace ActionGameDemo.UI
{
    public class CharacterPanel : MonoBehaviour
    {
        [SerializeField] private Text _nameText;
        [SerializeField] private Text _levelText;
        [SerializeField] private Text _expText;
        [SerializeField] private Slider _expSlider;
        [SerializeField] private Text _hpText;
        [SerializeField] private Slider _hpSlider;
        [SerializeField] private Text _attackText;
        [SerializeField] private Text _defenseText;
        [SerializeField] private Text _speedText;
        [SerializeField] private Text _critText;
        [SerializeField] private GameObject _panelRoot;

        private CharacterBase _character;
        private LevelSystem _levelSystem;
        private EquipmentManager _equipmentManager;

        private void Start()
        {
            if (_panelRoot != null) _panelRoot.SetActive(false);
            FindCharacter();
        }

        private void FindCharacter()
        {
            var player = FindFirstObjectByType<Player.PlayerController>();
            if (player != null)
            {
                _character = player.GetComponent<CharacterBase>();
                _levelSystem = player.GetComponent<LevelSystem>();
                _equipmentManager = player.GetComponent<EquipmentManager>();
            }
            else
            {
                Invoke(nameof(FindCharacter), 0.5f);
            }
        }

        private void Update()
        {
            if (_panelRoot == null || !_panelRoot.activeInHierarchy) return;
            if (_character == null) { FindCharacter(); return; }
            UpdateDisplay();
        }

        private void UpdateDisplay()
        {
            var stats = _character.Stats;
            if (stats == null) return;

            if (_nameText != null)
            {
                var charData = _character.GetCharacterData();
                _nameText.text = charData != null ? charData.characterName : "Player";
            }

            if (_levelSystem != null)
            {
                if (_levelText != null) _levelText.text = $"Lv.{_levelSystem.Level}";
                if (_expText != null) _expText.text = $"{(int)_levelSystem.CurrentExp}/{(int)_levelSystem.ExpToNextLevel}";
                if (_expSlider != null) _expSlider.value = _levelSystem.ExpProgress;
            }

            if (_hpText != null) _hpText.text = $"{(int)stats.CurrentHealth}/{(int)stats.MaxHealth}";
            if (_hpSlider != null) _hpSlider.value = stats.HealthPercent;
            if (_attackText != null) _attackText.text = $"ATK: {(int)stats.EffectiveAttackPower}";
            if (_defenseText != null) _defenseText.text = $"DEF: {(int)stats.EffectiveDefense}";
            if (_speedText != null) _speedText.text = $"SPD: {stats.EffectiveMoveSpeed:F1}";
            if (_critText != null) _critText.text = $"CRIT: {stats.EffectiveCriticalRate * 100f:F0}%";
        }

        public void TogglePanel()
        {
            if (_panelRoot != null)
                _panelRoot.SetActive(!_panelRoot.activeInHierarchy);
        }

        public void ShowPanel()
        {
            if (_panelRoot != null) _panelRoot.SetActive(true);
        }

        public void HidePanel()
        {
            if (_panelRoot != null) _panelRoot.SetActive(false);
        }
    }
}
