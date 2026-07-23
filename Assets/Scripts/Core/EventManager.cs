using System;
using UnityEngine;

namespace ActionGameDemo.Core
{
    /// <summary>
        /// 全局事件总线：负责模块间解耦通信，所有战斗、UI、状态变更均通过事件广播。
    /// 设计原则：
    ///   1. 静态事件 + 静态触发方法，任何脚本均可订阅/发布；
    ///   2. 事件参数传递完整上下文，避免订阅方反向查询；
    ///   3. 事件命名统一使用 OnXXX（订阅） / TriggerXXX（触发）。
    /// </summary>
    public class EventManager : MonoBehaviour
    {
        // ---------- 战斗事件 ----------
        /// <summary>角色受到伤害时触发（参数：角色对象, 伤害值, 当前生命）</summary>
        public static event Action<GameObject, float, float> OnTakeDamage;

        /// <summary>角色接受治疗时触发（参数：角色对象, 治疗量, 当前生命）</summary>
        public static event Action<GameObject, float, float> OnHeal;

        /// <summary>角色死亡时触发（参数：死亡角色对象）</summary>
        public static event Action<GameObject> OnDeath;

        /// <summary>结构化伤害事件（供后续伤害管线使用）</summary>
        public static event Action<Combat.DamageInfo> OnDamageTaken;

        // ---------- 状态事件 ----------
        /// <summary>游戏全局状态变更时触发</summary>
        public static event Action<GameManager.GameState> OnGameStateChanged;

        /// <summary>角色生命值百分比变更时触发（参数：当前百分比 0-1）</summary>
        public static event Action<float, float> OnHealthChanged;

        // ----------
        /// <summary>造成伤害时触发（attacker, target, damage, isCritical）</summary>
        public static event Action<GameObject, GameObject, float, bool> OnDamageDealt;

        /// <summary>暴击时触发（attacker, target, damage）</summary>
        public static event Action<GameObject, GameObject, float> OnCriticalHit;

        /// <summary>敌人死亡时触发（enemy）</summary>
        public static event Action<GameObject> OnEnemyKilled;

        /// <summary>玩家受伤时触发（player, damage）</summary>
        public static event Action<GameObject, float> OnPlayerHurt;

        /// <summary>战斗开始时触发</summary>
        public static event Action OnBattleStart;

        /// <summary>战斗结束时触发</summary>
        public static event Action OnBattleEnd;

        // ---------- 触发方法 ----------
        /// <summary>触发角色受伤事件</summary>
        public static void TriggerOnTakeDamage(GameObject character, float damage, float currentHealth)
        {
            if (character == null) return;
            OnTakeDamage?.Invoke(character, damage, currentHealth);
        }

        /// <summary>触发角色治疗事件</summary>
        public static void TriggerOnHeal(GameObject character, float amount, float currentHealth)
        {
            if (character == null) return;
            OnHeal?.Invoke(character, amount, currentHealth);
        }

        /// <summary>触发角色死亡事件</summary>
        public static void TriggerOnDeath(GameObject character)
        {
            if (character == null) return;
            OnDeath?.Invoke(character);
        }

        /// <summary>触发结构化伤害事件</summary>
        public static void TriggerDamageTaken(Combat.DamageInfo damageInfo)
        {
            OnDamageTaken?.Invoke(damageInfo);
        }

        /// <summary>触发游戏状态变更事件</summary>
        public static void TriggerOnGameStateChanged(GameManager.GameState newState)
        {
            OnGameStateChanged?.Invoke(newState);
        }

        /// <summary>触发生命值变更事件</summary>
        public static void TriggerHealthChanged(float current, float max)
        {
            OnHealthChanged?.Invoke(current, max);
        }

        // ----------
        /// <summary>触发造成伤害事件</summary>
        public static void TriggerOnDamageDealt(GameObject attacker, GameObject target, float damage, bool isCritical)
        {
            OnDamageDealt?.Invoke(attacker, target, damage, isCritical);
        }

        /// <summary>触发暴击事件</summary>
        public static void TriggerOnCriticalHit(GameObject attacker, GameObject target, float damage)
        {
            OnCriticalHit?.Invoke(attacker, target, damage);
        }

        /// <summary>触发敌人死亡事件</summary>
        public static void TriggerOnEnemyKilled(GameObject enemy)
        {
            OnEnemyKilled?.Invoke(enemy);
        }

        /// <summary>触发玩家受伤事件</summary>
        public static void TriggerOnPlayerHurt(GameObject player, float damage)
        {
            OnPlayerHurt?.Invoke(player, damage);
        }

        /// <summary>触发战斗开始事件</summary>
        public static void TriggerOnBattleStart()
        {
            OnBattleStart?.Invoke();
        }

        /// <summary>触发战斗结束事件</summary>
        public static void TriggerOnBattleEnd()
        {
            OnBattleEnd?.Invoke();
        }
    }
}
