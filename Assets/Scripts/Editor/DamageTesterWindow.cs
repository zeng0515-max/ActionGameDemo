using UnityEngine;
using UnityEditor;
using ActionGameDemo.Config;
using ActionGameDemo.Combat;

namespace ActionGameDemo.EditorTools
{
    public class DamageTesterWindow : EditorWindow
    {
        private float _attackPower = 20f;
        private float _defense = 5f;
        private float _skillMultiplier = 1.5f;
        private float _critRate = 0.1f;
        private float _critDamage = 1.5f;
        private ElementType _attackElement = ElementType.None;
        private ElementType _defenderElement = ElementType.None;
        private float _damageTakenMult = 1f;
        private string _resultText = "";

        [MenuItem("ActionGameDemo/伤害测试工具")]
        public static void ShowWindow()
        {
            GetWindow<DamageTesterWindow>("伤害测试");
        }

        private void OnGUI()
        {
            EditorGUILayout.LabelField("伤害计算测试", EditorStyles.boldLabel);

            EditorGUILayout.Space();
            _attackPower = EditorGUILayout.FloatField("攻击力", _attackPower);
            _defense = EditorGUILayout.FloatField("防御力", _defense);
            _skillMultiplier = EditorGUILayout.FloatField("技能倍率", _skillMultiplier);
            _critRate = EditorGUILayout.Slider("暴击率", _critRate, 0f, 1f);
            _critDamage = EditorGUILayout.FloatField("暴击倍率", _critDamage);
            _attackElement = (ElementType)EditorGUILayout.EnumPopup("攻击元素", _attackElement);
            _defenderElement = (ElementType)EditorGUILayout.EnumPopup("防御元素", _defenderElement);
            _damageTakenMult = EditorGUILayout.FloatField("受伤倍率", _damageTakenMult);

            EditorGUILayout.Space();
            if (GUILayout.Button("计算伤害", GUILayout.Height(30)))
            {
                CalculateDamage();
            }

            EditorGUILayout.Space();
            if (!string.IsNullOrEmpty(_resultText))
            {
                EditorGUILayout.HelpBox(_resultText, MessageType.Info);
            }
        }

        private void CalculateDamage()
        {
            DamageInfo info = new DamageInfo
            {
                baseDamage = _attackPower,
                skillMultiplier = _skillMultiplier,
                attacker = null
            };

            DamageInfo result = DamagePipeline.Calculate(
                info, _attackPower, _defense,
                _attackElement, _defenderElement,
                _critRate, _critDamage, _damageTakenMult);

            bool isCrit = result.isCritical;
            bool isCounter = ElementSystem.IsCountering(_attackElement, _defenderElement);

            _resultText = $"最终伤害: {result.finalDamage:F1}\n" +
                          $"暴击: {(isCrit ? "是" : "否")}\n" +
                          $"元素克制: {(isCounter ? "是 (150%)" : "否")}\n" +
                          $"元素倍率: {result.elementMultiplier:F2}\n" +
                          $"公式: ({_attackPower} × {_skillMultiplier}) × {(isCrit ? _critDamage : 1f)} × {result.elementMultiplier} - {_defense} × {_damageTakenMult} = {result.finalDamage:F1}";
        }
    }
}
