using UnityEngine;
using UnityEditor;
using ActionGameDemo.Combat.Buff;

namespace ActionGameDemo.EditorTools
{
    [CustomEditor(typeof(BuffData))]
    public class BuffDataEditor : UnityEditor.Editor
    {
        public override void OnInspectorGUI()
        {
            var data = (BuffData)target;

            EditorGUILayout.LabelField("Buff 配置", EditorStyles.boldLabel);
            data.buffName = EditorGUILayout.TextField("Buff 名称", data.buffName);
            data.buffType = (BuffType)EditorGUILayout.EnumPopup("Buff 类型", data.buffType);
            data.icon = (Sprite)EditorGUILayout.ObjectField("图标", data.icon, typeof(Sprite), false);
            data.description = EditorGUILayout.TextField("描述", data.description);

            EditorGUILayout.Space();
            EditorGUILayout.LabelField("持续时间与堆叠", EditorStyles.boldLabel);
            data.duration = EditorGUILayout.FloatField("持续时间", data.duration);
            data.stackingRule = (BuffStackingRule)EditorGUILayout.EnumPopup("堆叠规则", data.stackingRule);
            data.maxStacks = EditorGUILayout.IntField("最大堆叠", data.maxStacks);

            EditorGUILayout.Space();
            EditorGUILayout.LabelField("效果数值", EditorStyles.boldLabel);

            if (data.buffType == BuffType.DotBuff)
            {
                data.tickInterval = EditorGUILayout.FloatField("Tick 间隔", data.tickInterval);
                data.tickValuePercent = EditorGUILayout.FloatField("Tick 百分比", data.tickValuePercent);
            }
            else if (data.buffType == BuffType.AttributeBuff)
            {
                data.attributeType = (AttributeType)EditorGUILayout.EnumPopup("属性类型", data.attributeType);
                data.attributeModifier = EditorGUILayout.FloatField("修改百分比", data.attributeModifier);
            }
            else if (data.buffType == BuffType.ShieldBuff)
            {
                data.shieldValue = EditorGUILayout.FloatField("护盾值", data.shieldValue);
            }

            EditorGUILayout.Space();
            data.relatedElement = (Combat.ElementType)EditorGUILayout.EnumPopup("关联元素", data.relatedElement);

            if (GUI.changed) EditorUtility.SetDirty(data);
        }
    }
}
