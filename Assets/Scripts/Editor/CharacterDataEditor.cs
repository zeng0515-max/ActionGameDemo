using UnityEngine;
using UnityEditor;
using ActionGameDemo.Config;

namespace ActionGameDemo.EditorTools
{
    [CustomEditor(typeof(CharacterData))]
    public class CharacterDataEditor : UnityEditor.Editor
    {
        public override void OnInspectorGUI()
        {
            var data = (CharacterData)target;

            EditorGUILayout.LabelField("角色配置", EditorStyles.boldLabel);
            EditorGUI.indentLevel++;
            data.characterName = EditorGUILayout.TextField("角色名称", data.characterName);
            data.level = EditorGUILayout.IntField("等级", data.level);
            EditorGUI.indentLevel--;

            EditorGUILayout.Space();
            EditorGUILayout.LabelField("基础属性", EditorStyles.boldLabel);
            EditorGUI.indentLevel++;
            data.maxHealth = EditorGUILayout.FloatField("最大生命值", data.maxHealth);
            data.attackPower = EditorGUILayout.FloatField("攻击力", data.attackPower);
            data.defense = EditorGUILayout.FloatField("防御力", data.defense);
            data.moveSpeed = EditorGUILayout.FloatField("移动速度", data.moveSpeed);
            data.attackSpeed = EditorGUILayout.FloatField("攻击速度", data.attackSpeed);
            EditorGUI.indentLevel--;

            EditorGUILayout.Space();
            EditorGUILayout.LabelField("暴击属性", EditorStyles.boldLabel);
            EditorGUI.indentLevel++;
            data.criticalRate = EditorGUILayout.Slider("暴击率 (0-1)", data.criticalRate, 0f, 1f);
            data.criticalDamage = EditorGUILayout.FloatField("暴击伤害倍率", data.criticalDamage);
            EditorGUI.indentLevel--;

            if (GUI.changed) EditorUtility.SetDirty(data);
        }
    }
}
