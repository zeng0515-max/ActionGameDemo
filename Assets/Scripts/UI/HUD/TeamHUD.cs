using UnityEngine;
using UnityEngine.UI;
using ActionGameDemo.Player;
using ActionGameDemo.Character;

namespace ActionGameDemo.UI
{
    public class TeamHUD : MonoBehaviour
    {
        [SerializeField] private Transform _memberListParent;
        [SerializeField] private GameObject _memberPrefab;
        [SerializeField] private Color _activeColor = new Color(0.3f, 0.8f, 1f);
        [SerializeField] private Color _inactiveColor = new Color(0.5f, 0.5f, 0.5f);

        private CharacterSwitcher _switcher;

        private void OnEnable()
        {
            CharacterSwitcher.OnCharacterSwitched += OnCharacterSwitched;
        }

        private void OnDisable()
        {
            CharacterSwitcher.OnCharacterSwitched -= OnCharacterSwitched;
        }

        private void Start()
        {
            _switcher = FindFirstObjectByType<CharacterSwitcher>();
            if (_switcher != null && _switcher.Team != null)
                RebuildMemberList();
            else
                Invoke(nameof(TryFindSwitcher), 0.5f);
        }

        private void TryFindSwitcher()
        {
            _switcher = FindFirstObjectByType<CharacterSwitcher>();
            if (_switcher != null && _switcher.Team != null)
                RebuildMemberList();
            else
                Invoke(nameof(TryFindSwitcher), 0.5f);
        }

        private void RebuildMemberList()
        {
            if (_memberListParent == null || _memberPrefab == null || _switcher == null) return;

            foreach (Transform child in _memberListParent)
                Destroy(child.gameObject);

            for (int i = 0; i < _switcher.Team.MemberCount; i++)
            {
                GameObject item = Instantiate(_memberPrefab, _memberListParent);
                var member = _switcher.Team.GetMember(i);
                if (member == null) continue;

                var nameText = item.GetComponentInChildren<Text>();
                var stats = member.GetComponent<CharacterStats>();
                var slider = item.GetComponentInChildren<Slider>();

                if (nameText != null)
                {
                    var charData = member.GetComponent<CharacterBase>();
                    nameText.text = charData != null ? charData.GetCharacterData()?.characterName ?? $"Char {i + 1}" : $"Char {i + 1}";
                }

                if (slider != null && stats != null)
                    slider.value = stats.HealthPercent;
            }

            UpdateActiveHighlight();
        }

        private void OnCharacterSwitched(GameObject newCharacter)
        {
            UpdateActiveHighlight();
        }

        private void UpdateActiveHighlight()
        {
            if (_switcher == null || _switcher.Team == null || _memberListParent == null) return;

            for (int i = 0; i < _memberListParent.childCount && i < _switcher.Team.MemberCount; i++)
            {
                var child = _memberListParent.GetChild(i);
                bool isActive = _switcher.Team.CurrentIndex == i;

                var image = child.GetComponent<Image>();
                if (image != null)
                    image.color = isActive ? _activeColor : _inactiveColor;

                var stats = _switcher.Team.GetMember(i)?.GetComponent<CharacterStats>();
                var slider = child.GetComponentInChildren<Slider>();
                if (slider != null && stats != null)
                    slider.value = stats.HealthPercent;
            }
        }

        private void Update()
        {
            if (_switcher == null) return;
            UpdateActiveHighlight();
        }
    }
}
