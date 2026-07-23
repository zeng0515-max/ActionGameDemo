using UnityEngine;
using ActionGameDemo.Core;

namespace ActionGameDemo.Player
{
    public class CharacterSwitcher : MonoBehaviour
    {
        [Header("切换设置")]
        [SerializeField] private float _switchCooldown = 3f;
        [SerializeField] private float _invincibleDuration = 1f;

        [Header("队伍成员（拖入 Player GameObject）")]
        [SerializeField] private GameObject[] _teamMembers;

        private CharacterTeam _team;
        private float _lastSwitchTime = -999f;

        public CharacterTeam Team => _team;
        public GameObject CurrentCharacter => _team?.CurrentCharacter;
        public float SwitchCooldownRemaining => Mathf.Max(0f, _switchCooldown - (Time.time - _lastSwitchTime));
        public bool CanSwitch => _team != null && _team.CanSwitch && SwitchCooldownRemaining <= 0f;

        public static event System.Action<GameObject> OnCharacterSwitched;
        public static event System.Action<float> OnSwitchCooldownChanged;

        private void Start()
        {
            _team = new CharacterTeam();
            foreach (var member in _teamMembers)
                _team.AddMember(member);

            if (_team.CurrentCharacter != null)
                OnCharacterSwitched?.Invoke(_team.CurrentCharacter);
        }

        private void Update()
        {
            if (Input.GetKeyDown(KeyCode.Tab) && CanSwitch)
            {
                SwitchCharacter();
            }
        }

        public void SwitchCharacter()
        {
            if (!CanSwitch) return;

            GameObject previous = _team.CurrentCharacter;
            GameObject next = _team.SwitchToNext();

            if (next != null && next != previous)
            {
                _lastSwitchTime = Time.time;
                OnCharacterSwitched?.Invoke(next);

                // 设置无敌帧
                var characterBase = next.GetComponent<Character.CharacterBase>();
                if (characterBase != null)
                {
                    characterBase.SetInvincible(true);
                    StartCoroutine(RemoveInvincibleAfterDelay(characterBase, _invincibleDuration));
                }

                OnSwitchCooldownChanged?.Invoke(SwitchCooldownRemaining);
            }
        }

        private System.Collections.IEnumerator RemoveInvincibleAfterDelay(Character.CharacterBase character, float delay)
        {
            yield return new WaitForSeconds(delay);
            if (character != null && !character.IsDead)
                character.SetInvincible(false);
        }
    }
}
