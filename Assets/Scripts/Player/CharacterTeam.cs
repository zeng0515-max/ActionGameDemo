using System.Collections.Generic;
using UnityEngine;

namespace ActionGameDemo.Player
{
    public class CharacterTeam
    {
        private readonly List<GameObject> _members = new List<GameObject>();
        private int _currentIndex;

        public List<GameObject> Members => _members;
        public int CurrentIndex => _currentIndex;
        public GameObject CurrentCharacter => _members.Count > 0 ? _members[_currentIndex] : null;
        public int MemberCount => _members.Count;
        public bool CanSwitch => _members.Count > 1;

        public void AddMember(GameObject member)
        {
            if (member == null || _members.Contains(member)) return;
            _members.Add(member);
            member.SetActive(_members.Count == 1);
        }

        public GameObject SwitchToNext()
        {
            if (!CanSwitch) return CurrentCharacter;
            CurrentCharacter?.SetActive(false);
            _currentIndex = (_currentIndex + 1) % _members.Count;
            CurrentCharacter?.SetActive(true);
            return CurrentCharacter;
        }

        public GameObject SwitchTo(int index)
        {
            if (index < 0 || index >= _members.Count) return null;
            CurrentCharacter?.SetActive(false);
            _currentIndex = index;
            CurrentCharacter?.SetActive(true);
            return CurrentCharacter;
        }

        public void RemoveMember(GameObject member)
        {
            if (!_members.Contains(member)) return;
            int idx = _members.IndexOf(member);
            _members.Remove(member);
            if (_currentIndex >= _members.Count)
                _currentIndex = Mathf.Max(0, _members.Count - 1);
            if (_members.Count > 0)
                _members[_currentIndex].SetActive(true);
        }

        public GameObject GetMember(int index)
        {
            if (index < 0 || index >= _members.Count) return null;
            return _members[index];
        }
    }
}
