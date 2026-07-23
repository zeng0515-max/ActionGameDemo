using UnityEngine;
using System.Collections.Generic;

namespace ActionGameDemo.Core
{
    [System.Serializable]
    public class SaveData
    {
        public int version = 1;
        public string timestamp = "";

        [System.Serializable]
        public class CharacterSave
        {
            public string characterName = "";
            public int level = 1;
            public float currentExp = 0f;
            public float currentHealth = 100f;
            public string equippedWeapon = "";
            public string equippedArmor = "";
            public string equippedAccessory = "";
            public List<string> unlockedSkills = new List<string>();
            public int skillPoints = 0;
        }

        [System.Serializable]
        public class SettingsSave
        {
            public float masterVolume = 1f;
            public float musicVolume = 1f;
            public float sfxVolume = 1f;
            public int qualityLevel = 2;
        }

        public CharacterSave playerData = new CharacterSave();
        public List<string> teamMemberNames = new List<string>();
        public int currentTeamIndex = 0;
        public SettingsSave settings = new SettingsSave();

        public string ToJson() => JsonUtility.ToJson(this, true);
        public static SaveData FromJson(string json) => JsonUtility.FromJson<SaveData>(json);
    }
}
