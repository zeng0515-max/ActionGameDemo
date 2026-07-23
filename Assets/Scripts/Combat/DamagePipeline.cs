using UnityEngine;

namespace ActionGameDemo.Combat
{
    /// <summary>
        /// 伤害计算管线：按顺序执行各步骤计算，每步独立可追溯。
    /// 管线 5 步：
    ///   1. 基础伤害 = 攻击力 × 技能倍率
    ///   2. 暴击计算 = × 暴击倍率（if isCritical）
    ///   3. 元素克制 = × 元素倍率（ElementSystem 查询）
    ///   4. 防御减免 = max(1, 伤害 - 防御力)
    ///   5. 最终伤害 = max(1, 结果)
    /// </summary>
    public static class DamagePipeline
    {
        /// <summary>
        /// 执行完整伤害计算管线。
        /// 输入 DamageInfo 中的 baseDamage、skillMultiplier、isCritical、criticalDamage、attackElement、defenderElement、defense 会被使用。
        /// 输出 DamageInfo 的 finalDamage 被更新为最终伤害值，elementMultiplier 被更新为元素倍率。
        /// </summary>
        public static DamageInfo Calculate(DamageInfo input, float attackPower, float defense,
            ElementType attackElement, ElementType defenderElement,
            float criticalRate = 0f, float criticalDamageMultiplier = 1.5f,
            float damageTakenMultiplier = 1f)
        {
            DamageInfo result = input;
            result.attackElement = attackElement;
            result.defenderElement = defenderElement;
            result.defense = defense;

            // 步骤 1：基础伤害 = 攻击力 × 技能倍率
            float step1 = attackPower * result.skillMultiplier;
#if UNITY_EDITOR
            Debug.Log($"[DamagePipeline] Step1 基础伤害: {attackPower} × {result.skillMultiplier} = {step1}");
#endif

            // 步骤 2：暴击计算
            // 如果输入已标记 isCritical 则直接使用；否则按暴击率随机判定
            bool isCrit = result.isCritical;
            if (!isCrit && criticalRate > 0f)
            {
                isCrit = Random.value < criticalRate;
            }

            float step2 = step1;
            float critMult = isCrit ? criticalDamageMultiplier : 1f;
            step2 = step1 * critMult;
            result.isCritical = isCrit;
            result.criticalDamage = critMult;
#if UNITY_EDITOR
            if (isCrit)
                Debug.Log($"[DamagePipeline] Step2 暴击: {step1} × {critMult} = {step2}");
#endif

            // 步骤 3：元素克制
            float elemMult = ElementSystem.GetMultiplier(attackElement, defenderElement);
            result.elementMultiplier = elemMult;
            float step3 = step2 * elemMult;
#if UNITY_EDITOR
            if (elemMult > 1f)
                Debug.Log($"[DamagePipeline] Step3 元素克制: {step2} × {elemMult} = {step3} ({attackElement}→{defenderElement})");
#endif

            // 步骤 4：防御减免（防御力不为 0 时减免）
            float step4 = step3;
            if (defense > 0f)
            {
                step4 = step3 - defense;
            }
#if UNITY_EDITOR
            if (defense > 0f)
                Debug.Log($"[DamagePipeline] Step4 防御减免: {step3} - {defense} = {step4}");
#endif

            // 步骤 4.5：受伤加成倍率（感电等 Debuff）
            float step4b = step4 * damageTakenMultiplier;
#if UNITY_EDITOR
            if (damageTakenMultiplier != 1f)
                Debug.Log($"[DamagePipeline] Step4b 受伤加成: {step4} × {damageTakenMultiplier} = {step4b}");
#endif

            // 步骤 5：最终伤害 = max(1, 结果)
            float finalDamage = Mathf.Max(1f, step4b);
            result.finalDamage = finalDamage;
#if UNITY_EDITOR
            Debug.Log($"[DamagePipeline] Step5 最终伤害: max(1, {step4b}) = {finalDamage}");
#endif

            return result;
        }

        /// <summary>
        /// 简化版伤害计算（不含随机暴击，适用于已确定暴击的场景）。
        /// </summary>
        public static DamageInfo CalculateSimple(DamageInfo input, float attackPower, float defense,
            ElementType attackElement, ElementType defenderElement, float damageTakenMultiplier = 1f)
        {
            return Calculate(input, attackPower, defense, attackElement, defenderElement,
                0f, 1.5f, damageTakenMultiplier);
        }
    }
}
