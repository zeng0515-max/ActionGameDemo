using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Core;

namespace ActionGameDemo.Combat
{
    /// <summary>
        /// 全局战斗协调器：伤害事件分发、战斗统计、伤害日志、战斗节奏控制。
    /// 所有战斗相关事件通过 CombatCoordinator 中转，降低模块间耦合。
    /// </summary>
    public class CombatCoordinator : Singleton<CombatCoordinator>
    {
        // ---------- 事件 ----------
        /// <summary>造成伤害时触发（attacker, target, damage, isCritical）</summary>
        public static event System.Action<GameObject, GameObject, float, bool> OnDamageDealt;

        /// <summary>暴击时触发（attacker, target, damage）</summary>
        public static event System.Action<GameObject, GameObject, float> OnCriticalHit;

        /// <summary>敌人死亡时触发（enemy）</summary>
        public static event System.Action<GameObject> OnEnemyKilled;

        /// <summary>玩家受伤时触发（player, damage）</summary>
        public static event System.Action<GameObject, float> OnPlayerHurt;

        /// <summary>战斗开始时触发</summary>
        public static event System.Action OnBattleStart;

        /// <summary>战斗结束时触发</summary>
        public static event System.Action OnBattleEnd;

        // ---------- 战斗统计 ----------
        public float TotalDamage { get; private set; }
        public int KillCount { get; private set; }
        public int MaxCombo { get; private set; }
        public float BattleDuration { get; private set; }
        public bool IsInBattle { get; private set; }

        private float _battleStartTime;
        private int _currentCombo;

        // ---------- 伤害日志 ----------
        private readonly List<string> _damageLogs = new List<string>();
        private const int MAX_LOG_COUNT = 100;

        private void Update()
        {
            if (IsInBattle)
            {
                BattleDuration = Time.time - _battleStartTime;
            }
        }

        /// <summary>报告伤害</summary>
        public void ReportDamage(GameObject attacker, GameObject target, float damage, bool isCritical)
        {
            TotalDamage += damage;

            OnDamageDealt?.Invoke(attacker, target, damage, isCritical);

            if (isCritical)
            {
                OnCriticalHit?.Invoke(attacker, target, damage);
            }

            // 判断是否为玩家受伤
            if (target != null && target.CompareTag("Player"))
            {
                OnPlayerHurt?.Invoke(target, damage);
            }

            // 记录伤害日志
            string log = $"[{Time.time:F2}] {(attacker != null ? attacker.name : "Unknown")} → {(target != null ? target.name : "Unknown")} : {damage:F1}{(isCritical ? " (Crit)" : "")}";
            _damageLogs.Add(log);
            if (_damageLogs.Count > MAX_LOG_COUNT)
                _damageLogs.RemoveAt(0);
        }

        /// <summary>报告击杀</summary>
        public void ReportKill(GameObject enemy)
        {
            KillCount++;
            OnEnemyKilled?.Invoke(enemy);

#if UNITY_EDITOR
            Debug.Log($"[CombatCoordinator] 击杀: {enemy?.name}, 总击杀: {KillCount}");
#endif
        }

        /// <summary>报告连击</summary>
        public void ReportCombo(int combo)
        {
            if (combo > MaxCombo)
                MaxCombo = combo;
            _currentCombo = combo;
        }

        /// <summary>开始战斗</summary>
        public void StartBattle()
        {
            if (IsInBattle) return;

            IsInBattle = true;
            _battleStartTime = Time.time;
            TotalDamage = 0f;
            KillCount = 0;
            MaxCombo = 0;
            _currentCombo = 0;
            _damageLogs.Clear();

            OnBattleStart?.Invoke();
#if UNITY_EDITOR
            Debug.Log("[CombatCoordinator] 战斗开始");
#endif
        }

        /// <summary>结束战斗</summary>
        public void EndBattle()
        {
            if (!IsInBattle) return;

            IsInBattle = false;
            BattleDuration = Time.time - _battleStartTime;

            OnBattleEnd?.Invoke();
#if UNITY_EDITOR
            Debug.Log($"[CombatCoordinator] 战斗结束 | 总伤害: {TotalDamage:F0} | 击杀: {KillCount} | 最高连击: {MaxCombo} | 时长: {BattleDuration:F1}s");
#endif
        }

        /// <summary>获取伤害日志</summary>
        public IReadOnlyList<string> GetDamageLogs() => _damageLogs;

        /// <summary>重置统计</summary>
        public void ResetStats()
        {
            TotalDamage = 0f;
            KillCount = 0;
            MaxCombo = 0;
            _currentCombo = 0;
            _damageLogs.Clear();
        }
    }
}
