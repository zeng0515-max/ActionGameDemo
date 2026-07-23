using UnityEngine;

namespace ActionGameDemo.Progression
{
    public enum EquipmentSlotType { Weapon, Armor, Accessory }
    public enum Rarity { Common, Rare, Epic, Legendary }

    [CreateAssetMenu(fileName = "EquipmentData", menuName = "ActionGameDemo/Equipment Data")]
    public class EquipmentData : ScriptableObject
    {
        [Header("基础信息")]
        public string itemName = "New Equipment";
        public EquipmentSlotType slotType = EquipmentSlotType.Weapon;
        public Rarity rarity = Rarity.Common;
        public Sprite icon;
        [TextArea] public string description = "";

        [Header("属性加成")]
        public float attackBonus = 0f;
        public float defenseBonus = 0f;
        public float healthBonus = 0f;
        public float criticalRateBonus = 0f;
        public float moveSpeedBonus = 0f;

        [Header("成长系数")]
        public float attackGrowth = 0f;
        public float defenseGrowth = 0f;

        public string GetRarityColor()
        {
            return rarity switch
            {
                Rarity.Common => "#FFFFFF",
                Rarity.Rare => "#3498db",
                Rarity.Epic => "#9b59b6",
                Rarity.Legendary => "#f39c12",
                _ => "#FFFFFF"
            };
        }
    }
}
