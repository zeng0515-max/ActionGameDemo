package com.actiongame.server.config;

import com.actiongame.server.domain.progression.EquipmentSlotType;
import com.actiongame.server.domain.progression.Rarity;

/**
 * 装备配置 (对应Unity EquipmentData ScriptableObject)
 * 从JSON加载, 服务端权威
 */
public class EquipmentConfig {
    private String itemName = "New Equipment";
    private EquipmentSlotType slotType = EquipmentSlotType.WEAPON;
    private Rarity rarity = Rarity.COMMON;
    private String description = "";
    private float attackBonus = 0f;
    private float defenseBonus = 0f;
    private float healthBonus = 0f;
    private float criticalRateBonus = 0f;
    private float moveSpeedBonus = 0f;
    private float attackGrowth = 0f;
    private float defenseGrowth = 0f;

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public EquipmentSlotType getSlotType() { return slotType; }
    public void setSlotType(EquipmentSlotType slotType) { this.slotType = slotType; }
    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public float getAttackBonus() { return attackBonus; }
    public void setAttackBonus(float attackBonus) { this.attackBonus = attackBonus; }
    public float getDefenseBonus() { return defenseBonus; }
    public void setDefenseBonus(float defenseBonus) { this.defenseBonus = defenseBonus; }
    public float getHealthBonus() { return healthBonus; }
    public void setHealthBonus(float healthBonus) { this.healthBonus = healthBonus; }
    public float getCriticalRateBonus() { return criticalRateBonus; }
    public void setCriticalRateBonus(float criticalRateBonus) { this.criticalRateBonus = criticalRateBonus; }
    public float getMoveSpeedBonus() { return moveSpeedBonus; }
    public void setMoveSpeedBonus(float moveSpeedBonus) { this.moveSpeedBonus = moveSpeedBonus; }
    public float getAttackGrowth() { return attackGrowth; }
    public void setAttackGrowth(float attackGrowth) { this.attackGrowth = attackGrowth; }
    public float getDefenseGrowth() { return defenseGrowth; }
    public void setDefenseGrowth(float defenseGrowth) { this.defenseGrowth = defenseGrowth; }

    @Override
    public String toString() {
        return "EquipmentConfig{name=" + itemName + ", slot=" + slotType + ", rarity=" + rarity + "}";
    }
}
