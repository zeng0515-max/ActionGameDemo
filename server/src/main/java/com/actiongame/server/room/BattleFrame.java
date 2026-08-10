package com.actiongame.server.room;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.MonsterCharacter;
import com.actiongame.server.domain.character.PlayerCharacter;
import com.actiongame.server.domain.combat.ElementType;
import com.actiongame.server.util.Vector3;
import com.actiongame.server.util.Quaternion;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗帧快照 (对应文档3.5 BattleFrame + proto定义)
 * 每帧由服务端生成, 包含所有角色状态和事件
 * 客户端通过帧快照驱动表现
 */
public class BattleFrame {
    private final long frameIndex;
    private final long timestamp;
    private final String roomId;
    private final List<CharacterSnapshot> characters;
    private final List<DamageNumberEvent> damageEvents;
    private final List<VFXEvent> vfxEvents;

    public BattleFrame(long frameIndex, long timestamp, String roomId) {
        this.frameIndex = frameIndex;
        this.timestamp = timestamp;
        this.roomId = roomId;
        this.characters = new ArrayList<>();
        this.damageEvents = new ArrayList<>();
        this.vfxEvents = new ArrayList<>();
    }

    /**
     * 角色快照 (对应proto CharacterSnapshot)
     */
    public static class CharacterSnapshot {
        public final int entityId;
        public final int configId;
        public final int entityType; // 0=player, 1=monster, 2=boss
        public final Vector3 position;
        public final Quaternion rotation;
        public final int state; // CharacterState enum value
        public final CharacterStatsSnapshot stats;
        public final List<BuffSnapshot> buffs;
        public final int comboStep;
        public final boolean isInvincible;
        public final int targetEntityId;

        public CharacterSnapshot(int entityId, int configId, int entityType,
                                 Vector3 position, Quaternion rotation, int state,
                                 CharacterStatsSnapshot stats, List<BuffSnapshot> buffs,
                                 int comboStep, boolean isInvincible, int targetEntityId) {
            this.entityId = entityId;
            this.configId = configId;
            this.entityType = entityType;
            this.position = position;
            this.rotation = rotation;
            this.state = state;
            this.stats = stats;
            this.buffs = buffs != null ? buffs : new ArrayList<>();
            this.comboStep = comboStep;
            this.isInvincible = isInvincible;
            this.targetEntityId = targetEntityId;
        }
    }

    /**
     * 角色属性快照 (对应proto CharacterStatsSnapshot, 只读下发)
     */
    public static class CharacterStatsSnapshot {
        public final float maxHealth;
        public final float currentHealth;
        public final float maxEnergy;
        public final float currentEnergy;
        public final float attackPower;
        public final float defense;
        public final float moveSpeed;
        public final float criticalRate;
        public final float criticalDamageMultiplier;
        public final float shieldAmount;

        public CharacterStatsSnapshot(CharacterStats stats) {
            this.maxHealth = stats.getMaxHealth();
            this.currentHealth = stats.getCurrentHealth();
            this.maxEnergy = stats.getMaxEnergy();
            this.currentEnergy = stats.getCurrentEnergy();
            this.attackPower = stats.getEffectiveAttackPower();
            this.defense = stats.getEffectiveDefense();
            this.moveSpeed = stats.getEffectiveMoveSpeed();
            this.criticalRate = stats.getEffectiveCriticalRate();
            this.criticalDamageMultiplier = stats.getCriticalDamageMultiplier();
            this.shieldAmount = 0f; // 由Character.getShieldAmount提供
        }
    }

    /**
     * Buff快照 (对应proto BuffSnapshot)
     */
    public static class BuffSnapshot {
        public final String buffId;
        public final String buffName;
        public final int buffType;
        public final float remainingTime;
        public final int stacks;
        public final int element;

        public BuffSnapshot(Buff buff) {
            this.buffId = String.valueOf(buff.getBuffId());
            this.buffName = buff.getBuffName();
            this.buffType = buff.getBuffType().getValue();
            this.remainingTime = buff.getRemainingTime();
            this.stacks = buff.getStacks();
            this.element = buff.getRelatedElement().getValue();
        }
    }

    /**
     * 伤害飘字事件 (对应proto DamageNumberEvent)
     */
    public static class DamageNumberEvent {
        public final int targetEntityId;
        public final float damage;
        public final boolean isCritical;
        public final int element;
        public final Vector3 position;

        public DamageNumberEvent(int targetEntityId, float damage, boolean isCritical,
                                 ElementType element, Vector3 position) {
            this.targetEntityId = targetEntityId;
            this.damage = damage;
            this.isCritical = isCritical;
            this.element = element.getValue();
            this.position = position;
        }
    }

    /**
     * 特效事件 (对应proto VFXEvent)
     */
    public static class VFXEvent {
        public final int vfxType;
        public final Vector3 position;
        public final Quaternion rotation;
        public final int followEntityId;
        public final float duration;

        public VFXEvent(int vfxType, Vector3 position, Quaternion rotation,
                        int followEntityId, float duration) {
            this.vfxType = vfxType;
            this.position = position;
            this.rotation = rotation;
            this.followEntityId = followEntityId;
            this.duration = duration;
        }
    }

    /**
     * 从Character构建快照
     */
    public void addCharacterSnapshot(Character character, int entityType) {
        List<BuffSnapshot> buffSnapshots = new ArrayList<>();
        for (Buff buff : character.getBuffs()) {
            buffSnapshots.add(new BuffSnapshot(buff));
        }

        CharacterStatsSnapshot statsSnap = new CharacterStatsSnapshot(character.getStats());

        CharacterSnapshot snap = new CharacterSnapshot(
            character.getEntityId(),
            character.getConfigId(),
            entityType,
            character.getPosition(),
            character.getRotation(),
            character.getState().getValue(),
            statsSnap,
            buffSnapshots,
            character.getComboStep(),
            character.isInvincible(),
            character.getTargetEntityId()
        );
        characters.add(snap);
    }

    public void addDamageEvent(DamageNumberEvent event) { damageEvents.add(event); }
    public void addVFXEvent(VFXEvent event) { vfxEvents.add(event); }

    public long getFrameIndex() { return frameIndex; }
    public long getTimestamp() { return timestamp; }
    public String getRoomId() { return roomId; }
    public List<CharacterSnapshot> getCharacters() { return characters; }
    public List<DamageNumberEvent> getDamageEvents() { return damageEvents; }
    public List<VFXEvent> getVfxEvents() { return vfxEvents; }
}
