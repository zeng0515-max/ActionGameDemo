using UnityEngine;

namespace ActionGameDemo.Progression
{
    public class EquipmentSlot
    {
        private EquipmentData _equippedItem;
        private readonly EquipmentSlotType _slotType;

        public EquipmentSlotType SlotType => _slotType;
        public EquipmentData EquippedItem => _equippedItem;
        public bool HasItem => _equippedItem != null;

        public EquipmentSlot(EquipmentSlotType slotType)
        {
            _slotType = slotType;
        }

        public EquipmentData Equip(EquipmentData item)
        {
            if (item == null || item.slotType != _slotType) return null;
            EquipmentData previous = _equippedItem;
            _equippedItem = item;
            return previous;
        }

        public EquipmentData Unequip()
        {
            EquipmentData removed = _equippedItem;
            _equippedItem = null;
            return removed;
        }
    }
}
