using UnityEngine;
using ActionGameDemo.Character;

namespace ActionGameDemo.Progression
{
    public class EquipmentManager : MonoBehaviour
    {
        [SerializeField] private CharacterStats _stats;

        private readonly EquipmentSlot _weaponSlot = new EquipmentSlot(EquipmentSlotType.Weapon);
        private readonly EquipmentSlot _armorSlot = new EquipmentSlot(EquipmentSlotType.Armor);
        private readonly EquipmentSlot _accessorySlot = new EquipmentSlot(EquipmentSlotType.Accessory);

        public EquipmentSlot WeaponSlot => _weaponSlot;
        public EquipmentSlot ArmorSlot => _armorSlot;
        public EquipmentSlot AccessorySlot => _accessorySlot;

        public event System.Action OnEquipmentChanged;

        private void Awake()
        {
            if (_stats == null) _stats = GetComponent<CharacterStats>();
        }

        public void Equip(EquipmentData item)
        {
            if (item == null || _stats == null) return;

            var slot = GetSlot(item.slotType);
            if (slot == null) return;

            var previous = slot.Equip(item);
            if (previous != null) RemoveBonuses(previous);
            ApplyBonuses(item);

            OnEquipmentChanged?.Invoke();
        }

        public void Unequip(EquipmentSlotType slotType)
        {
            var slot = GetSlot(slotType);
            if (slot == null || !slot.HasItem) return;

            var removed = slot.Unequip();
            if (removed != null) RemoveBonuses(removed);

            OnEquipmentChanged?.Invoke();
        }

        private EquipmentSlot GetSlot(EquipmentSlotType type)
        {
            return type switch
            {
                EquipmentSlotType.Weapon => _weaponSlot,
                EquipmentSlotType.Armor => _armorSlot,
                EquipmentSlotType.Accessory => _accessorySlot,
                _ => null
            };
        }

        private void ApplyBonuses(EquipmentData item)
        {
            if (_stats == null) return;
            if (item.attackBonus > 0f) _stats.AddAttackModifier(item.attackBonus / 100f);
            if (item.defenseBonus > 0f) _stats.AddDefenseModifier(item.defenseBonus / 100f);
            if (item.moveSpeedBonus > 0f) _stats.AddMoveSpeedModifier(item.moveSpeedBonus / 100f);
            if (item.criticalRateBonus > 0f) _stats.AddCriticalRateModifier(item.criticalRateBonus / 100f);
            if (item.healthBonus > 0f)
            {
                _stats.SetMaxHealth(_stats.MaxHealth + item.healthBonus);
                _stats.ModifyHealth(item.healthBonus);
            }
        }

        private void RemoveBonuses(EquipmentData item)
        {
            if (_stats == null) return;
            if (item.attackBonus > 0f) _stats.AddAttackModifier(-item.attackBonus / 100f);
            if (item.defenseBonus > 0f) _stats.AddDefenseModifier(-item.defenseBonus / 100f);
            if (item.moveSpeedBonus > 0f) _stats.AddMoveSpeedModifier(-item.moveSpeedBonus / 100f);
            if (item.criticalRateBonus > 0f) _stats.AddCriticalRateModifier(-item.criticalRateBonus / 100f);
            if (item.healthBonus > 0f)
            {
                _stats.SetMaxHealth(Mathf.Max(1f, _stats.MaxHealth - item.healthBonus));
            }
        }
    }
}
