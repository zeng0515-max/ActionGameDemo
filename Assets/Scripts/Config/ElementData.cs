using UnityEngine;
using ActionGameDemo.Combat;

namespace ActionGameDemo.Config
{
    /// <summary>
        /// 元素属性配置：克制关系、标识颜色。
    /// </summary>
    [CreateAssetMenu(fileName = "ElementData", menuName = "ActionGameDemo/Element Data")]
    public class ElementData : ScriptableObject
    {
        [Tooltip("元素类型")] public ElementType elementType = ElementType.None;
        [Tooltip("克制元素")] public ElementType counterElement = ElementType.None;
        [Tooltip("克制倍率")] public float counterMultiplier = 1.5f;
        [Tooltip("元素标识颜色")] public Color elementColor = Color.white;
        [Tooltip("元素名称")] public string elementName = "";
    }
}
