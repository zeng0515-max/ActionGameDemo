package com.actiongame.server.domain.progression;

import com.actiongame.server.config.EquipmentConfig;

/**
 * 装备槽 (服务端权威)
 * 对应Unity EquipmentSlot
 */
public class EquipmentSlot {
    private final EquipmentSlotType slotType;
    private EquipmentConfig equippedItem;

    public EquipmentSlot(EquipmentSlotType slotType) {
        this.slotType = slotType;
    }

    public EquipmentConfig equip(EquipmentConfig item) {
        if (item == null || item.getSlotType() != slotType) return null;
        EquipmentConfig previous = equippedItem;
        equippedItem = item;
        return previous;
    }

    public EquipmentConfig unequip() {
        EquipmentConfig removed = equippedItem;
        equippedItem = null;
        return removed;
    }

    public EquipmentSlotType getSlotType() { return slotType; }
    public EquipmentConfig getEquippedItem() { return equippedItem; }
    public boolean hasItem() { return equippedItem != null; }
}
