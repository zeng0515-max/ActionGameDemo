using UnityEngine;

namespace ActionGameDemo.Config
{
    [CreateAssetMenu(fileName = "CharacterData", menuName = "ActionGameDemo/Character Data")]
    public class CharacterData : ScriptableObject
    {
        public string characterName;
        public int level;
        public float maxHealth;
        public float attackPower;
        public float defense;
        public float moveSpeed;
        public float attackSpeed;
        public float criticalRate;
        public float criticalDamage;
    }
}
