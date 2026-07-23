using UnityEngine;
using ActionGameDemo.Combat;
using ActionGameDemo.Combat.Buff;
using ActionGameDemo.Config;
using ActionGameDemo.Core;

namespace ActionGameDemo.Character
{
    [RequireComponent(typeof(CharacterStats))]
    public class CharacterBase : MonoBehaviour, IDamageable
    {
        [Header("角色配置")]
        [SerializeField, Tooltip("角色基础数据配置表")] private CharacterData _characterData;

        [Header("元素属性")]
        [SerializeField, Tooltip("角色元素属性（用于元素克制计算）")] private ElementType _elementType = ElementType.None;

        private CharacterStats _stats;
        private BuffSystem _buffSystem;
        private bool _isDead;
        private bool _isInvincible;
        private float _shieldAmount;

        public CharacterStats Stats => _stats;
        public BuffSystem BuffSystem => _buffSystem;
        public bool IsDead => _isDead;
        public bool IsInvincible => _isInvincible;
        public ElementType ElementType => _elementType;
        public float ShieldAmount => _shieldAmount;

        protected virtual void Awake()
        {
            _stats = GetComponent<CharacterStats>();
            if (_stats == null)
            {
                Debug.LogError($"[CharacterBase] {gameObject.name} 缺少 CharacterStats 组件！");
                return;
            }
            InitializeStats();
            InitializeBuffSystem();
        }

        private void InitializeStats()
        {
            if (_characterData == null)
            {
                Debug.LogWarning($"[CharacterBase] {gameObject.name} 未分配 CharacterData，使用默认属性。");
                _stats.SetMaxHealth(100f);
                _stats.SetCurrentHealth(100f);
                return;
            }
            _stats.SetMaxHealth(_characterData.maxHealth);
            _stats.SetCurrentHealth(_characterData.maxHealth);
        }

        private void InitializeBuffSystem()
        {
            _buffSystem = GetComponent<BuffSystem>();
            if (_buffSystem == null)
            {
                _buffSystem = gameObject.AddComponent<BuffSystem>();
            }
        }

        /// <summary>获取元素属性（IDamageable 接口实现）</summary>
        public virtual ElementType GetElementType()
        {
            return _elementType;
        }

        public virtual void TakeDamage(float damage)
        {
            if (_isDead || _isInvincible || _stats == null)
                return;

            
            if (_shieldAmount > 0f)
            {
                if (damage <= _shieldAmount)
                {
                    _shieldAmount -= damage;
                    return;
                }
                else
                {
                    damage -= _shieldAmount;
                    _shieldAmount = 0f;
                }
            }

            if (damage < 0f)
            {
                Debug.LogWarning($"[CharacterBase] 收到负伤害 {damage}，已取反处理。");
                damage = -damage;
            }

            // 至少 0 点伤害（管线已保证 min 1，此处不强制 1 以兼容 DoT 0 伤害场景）
            damage = Mathf.Max(0f, damage);

            if (damage <= 0f) return;

            _stats.ModifyHealth(-damage);
            float currentHealth = _stats.CurrentHealth;

            EventManager.TriggerOnTakeDamage(gameObject, damage, currentHealth);

            

#if UNITY_EDITOR
            Debug.Log($"[CharacterBase] {gameObject.name} 受到 {damage} 点伤害，剩余生命: {currentHealth}");
#endif

            if (currentHealth <= 0f && !_isDead)
            {
                Die();
            }
        }

        public virtual void TakeDamage(DamageInfo damageInfo)
        {
            if (!damageInfo.IsValid)
            {
                Debug.LogError("[CharacterBase] 收到无效的 DamageInfo！");
                return;
            }

            float previousHealth = _stats != null ? _stats.CurrentHealth : 0f;

            if (damageInfo.finalDamage > 0f)
            {
                // 已有 finalDamage，直接使用（攻击方已完成管线计算）
                TakeDamage(damageInfo.finalDamage);
                EventManager.TriggerDamageTaken(damageInfo);
            }
            else
            {
                // finalDamage 未计算，使用 DamagePipeline 完整计算
                float attackPower = 10f;
                CharacterStats attackerStats = null;
                if (damageInfo.attacker != null)
                {
                    attackerStats = damageInfo.attacker.GetComponent<CharacterStats>();
                    if (attackerStats != null)
                        attackPower = attackerStats.EffectiveAttackPower;
                }

                ElementType defenderElement = GetElementType();
                float defense = _stats != null ? _stats.EffectiveDefense : 0f;
                float damageTakenMult = _stats != null ? _stats.DamageTakenMultiplier : 1f;

                float critRate = attackerStats != null ? attackerStats.EffectiveCriticalRate : 0f;
                float critMult = attackerStats != null ? attackerStats.CriticalDamageMultiplier : 1.5f;

                DamageInfo result = DamagePipeline.Calculate(
                    damageInfo, attackPower, defense,
                    damageInfo.attackElement, defenderElement,
                    critRate, critMult, damageTakenMult);

                damageInfo = result;
                TakeDamage(result.finalDamage);
                EventManager.TriggerDamageTaken(result);
            }

            // 上报到 CombatCoordinator（仅实际上造成了伤害时）
            float actualDamage = previousHealth - (_stats != null ? _stats.CurrentHealth : 0f);
            if (actualDamage > 0f && damageInfo.attacker != null)
            {
                CombatCoordinator.Instance?.ReportDamage(damageInfo.attacker, gameObject, actualDamage, damageInfo.isCritical);
            }
        }

        public virtual void Heal(float amount)
        {
            if (_isDead || amount <= 0f || _stats == null)
                return;

            _stats.ModifyHealth(amount);
            EventManager.TriggerOnHeal(gameObject, amount, _stats.CurrentHealth);

            
            VFXEffectManager.Instance?.PlayEffectFollow(VFXEffectType.Heal, transform, Vector3.up * 0.5f);
        }

        // ----------
        public void AddShield(float amount)
        {
            _shieldAmount += amount;
        }

        public void RemoveShield(float amount)
        {
            _shieldAmount = Mathf.Max(0f, _shieldAmount - amount);
        }

        // ----------
        /// <summary>添加 Buff</summary>
        public BuffBase AddBuff(BuffData buffData, GameObject source = null)
        {
            if (_buffSystem == null || _isDead) return null;
            return _buffSystem.AddBuff(buffData, source);
        }

        /// <summary>移除指定名称的 Buff</summary>
        public void RemoveBuff(string buffName)
        {
            _buffSystem?.RemoveBuffByName(buffName);
        }

        /// <summary>清除所有 Buff</summary>
        public void ClearAllBuffs()
        {
            _buffSystem?.ClearAllBuffs();
            if (_stats != null)
                _stats.ResetAllModifiers();
        }

        protected virtual void Die()
        {
            _isDead = true;

            
            ClearAllBuffs();

            // 上报击杀（如果是敌人）
            if (gameObject.CompareTag("Enemy"))
            {
                CombatCoordinator.Instance?.ReportKill(gameObject);
            }

            EventManager.TriggerOnDeath(gameObject);

            
            VFXEffectManager.Instance?.PlayEffect(VFXEffectType.Death, transform.position + Vector3.up, Quaternion.identity);
#if UNITY_EDITOR
            Debug.Log($"[CharacterBase] {gameObject.name} 已死亡。");
#endif
            _destroyCoroutine = StartCoroutine(DestroyAfterDelay(2f));
        }

        private UnityEngine.Coroutine _destroyCoroutine;

        private System.Collections.IEnumerator DestroyAfterDelay(float delay)
        {
            yield return new WaitForSeconds(delay);
            Destroy(gameObject);
        }

        /// <summary>取消待销毁（复活时调用）</summary>
        public void CancelPendingDestroy()
        {
            if (_destroyCoroutine != null)
            {
                StopCoroutine(_destroyCoroutine);
                _destroyCoroutine = null;
            }
        }

        public void SetInvincible(bool invincible)
        {
            _isInvincible = invincible;
        }

        public CharacterData GetCharacterData() => _characterData;

        public void SetCharacterData(CharacterData data)
        {
            _characterData = data;
            InitializeStats();
        }
    }
}
