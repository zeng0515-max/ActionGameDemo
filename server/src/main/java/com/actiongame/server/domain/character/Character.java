package com.actiongame.server.domain.character;

import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.combat.ElementType;
import com.actiongame.server.util.Vector3;
import com.actiongame.server.util.Quaternion;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 角色实体基类 (服务端权威)
 * 对应Unity CharacterBase, 但去除MonoBehaviour依赖
 * 存储所有权威数值, 客户端仅通过帧快照读取
 */
public abstract class Character {
    protected int entityId;
    protected final int configId;
    protected final CharacterStats stats;
    protected CharacterState state = CharacterState.IDLE;
    protected Vector3 position = Vector3.ZERO;
    protected Quaternion rotation = Quaternion.IDENTITY;
    protected ElementType elementType = ElementType.NONE;
    protected float shieldAmount = 0f;
    protected boolean isDead = false;
    protected boolean isInvincible = false;
    protected int targetEntityId = -1;
    protected int comboStep = 0;
    protected final List<Buff> buffs = new CopyOnWriteArrayList<>();

    protected Character(int entityId, int configId, CharacterStats stats) {
        this.entityId = entityId;
        this.configId = configId;
        this.stats = stats;
    }

    /**
     * 受到伤害 (服务端权威, 客户端不可调用)
     */
    public void takeDamage(float damage) {
        if (isDead || isInvincible) return;

        // 护盾吸收
        if (shieldAmount > 0f) {
            if (damage <= shieldAmount) {
                shieldAmount -= damage;
                return;
            } else {
                damage -= shieldAmount;
                shieldAmount = 0f;
            }
        }

        damage = Math.max(0f, damage);
        if (damage <= 0f) return;

        stats.modifyHealth(-damage);

        if (stats.getCurrentHealth() <= 0f && !isDead) {
            die();
        }
    }

    /**
     * 治疗
     */
    public void heal(float amount) {
        if (isDead || amount <= 0f) return;
        stats.modifyHealth(amount);
    }

    /**
     * 添加护盾
     */
    public void addShield(float amount) {
        shieldAmount += amount;
    }

    /**
     * 移除护盾
     */
    public void removeShield(float amount) {
        shieldAmount = Math.max(0f, shieldAmount - amount);
    }

    /**
     * 添加Buff
     */
    public void addBuff(Buff buff) {
        if (isDead) return;
        buffs.add(buff);
    }

    /**
     * 移除Buff
     */
    public void removeBuff(Buff buff) {
        buffs.remove(buff);
    }

    /**
     * 清除所有Buff
     */
    public void clearAllBuffs() {
        buffs.clear();
        stats.resetAllModifiers();
    }

    protected void die() {
        isDead = true;
        state = CharacterState.DEAD;
        clearAllBuffs();
    }

    // === Getters / Setters ===

    public int getEntityId() { return entityId; }
    public void setEntityId(int entityId) { this.entityId = entityId; }
    public int getConfigId() { return configId; }
    public CharacterStats getStats() { return stats; }
    public CharacterState getState() { return state; }
    public void setState(CharacterState state) { this.state = state; }
    public Vector3 getPosition() { return position; }
    public void setPosition(Vector3 position) { this.position = position; }
    public Quaternion getRotation() { return rotation; }
    public void setRotation(Quaternion rotation) { this.rotation = rotation; }
    public ElementType getElementType() { return elementType; }
    public void setElementType(ElementType elementType) { this.elementType = elementType; }
    public float getShieldAmount() { return shieldAmount; }
    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) {
        this.isDead = dead;
        if (dead) {
            state = CharacterState.DEAD;
            stats.setCurrentHealth(0f);
            clearAllBuffs();
        }
    }
    public boolean isInvincible() { return isInvincible; }
    public void setInvincible(boolean invincible) { isInvincible = invincible; }
    public int getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(int targetEntityId) { this.targetEntityId = targetEntityId; }
    public int getComboStep() { return comboStep; }
    public void setComboStep(int comboStep) { this.comboStep = comboStep; }
    public List<Buff> getBuffs() { return buffs; }
}
