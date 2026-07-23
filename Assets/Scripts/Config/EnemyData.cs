using UnityEngine;

namespace ActionGameDemo.Config
{
    [CreateAssetMenu(fileName = "EnemyData", menuName = "ActionGameDemo/Enemy Data")]
    public class EnemyData : ScriptableObject
    {
        [Header("基础属性")]
        public string enemyName = "Enemy";
        public float maxHealth = 50f;
        public float attackPower = 5f;
        public float defense = 2f;
        public float moveSpeed = 3f;

        [Header("AI 参数")]
        public float detectionRange = 10f;
        public float attackRange = 2f;
        public float attackCooldown = 3f;
        public float patrolRadius = 5f;
        public float fleeThreshold = 0.2f;
        public float viewAngle = 90f;
    }
}
