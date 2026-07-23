using UnityEngine;
using UnityEditor;
using ActionGameDemo.Config;

namespace ActionGameDemo.EditorTools
{
    [CustomEditor(typeof(SkillData))]
    public class SkillDataEditor : UnityEditor.Editor
    {
        public override void OnInspectorGUI()
        {
            var data = (SkillData)target;

            EditorGUILayout.LabelField("技能配置", EditorStyles.boldLabel);
            data.skillName = EditorGUILayout.TextField("技能名称", data.skillName);
            data.animationTriggerName = EditorGUILayout.TextField("动画触发器名", data.animationTriggerName);

            EditorGUILayout.Space();
            EditorGUILayout.LabelField("战斗参数", EditorStyles.boldLabel);
            data.damageMultiplier = EditorGUILayout.FloatField("伤害倍率", data.damageMultiplier);
            data.cooldown = EditorGUILayout.FloatField("冷却时间", data.cooldown);
            data.range = EditorGUILayout.FloatField("范围", data.range);

            EditorGUILayout.Space();
            EditorGUILayout.LabelField("连段参数", EditorStyles.boldLabel);
            data.comboIndex = EditorGUILayout.IntField("连段索引", data.comboIndex);
            data.canCancel = EditorGUILayout.Toggle("可取消", data.canCancel);

            if (GUI.changed) EditorUtility.SetDirty(data);
        }
    }
}
