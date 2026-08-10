package com.actiongame.server.room;

import com.actiongame.server.net.util.BinaryCodec;
import com.actiongame.server.util.Quaternion;
import com.actiongame.server.util.Vector3;

import java.util.List;

/**
 * BattleFrame → 客户端二进制序列化
 * 格式与客户端 BattleFrameNotify.FromByteArray 完全匹配
 */
public final class BattleFrameSerializer {

    private BattleFrameSerializer() {}

    /**
     * 序列化 BattleFrame 为客户端二进制格式
     */
    public static byte[] serialize(BattleFrame frame, int protocolVersion) {
        // 先写入固定头部, 然后逐个写入角色/事件
        // 使用动态估算大小
        List<BattleFrame.CharacterSnapshot> chars = frame.getCharacters();
        List<BattleFrame.DamageNumberEvent> dmgs = frame.getDamageEvents();
        List<BattleFrame.VFXEvent> vfxs = frame.getVfxEvents();

        // 估算最大大小
        int estimatedSize = 4 + 8 + 8 + 4 + 4 + 4; // header + roomId + counts
        for (var c : chars) estimatedSize += estimateCharSize(c);
        for (var d : dmgs) estimatedSize += 4 + 4 + 1 + 1 + 12; // damage event
        for (var v : vfxs) estimatedSize += 1 + 12 + 16 + 4 + 4; // vfx event

        byte[] buffer = new byte[estimatedSize + 256]; // extra padding
        int offset = 0;

        // Header
        BinaryCodec.writeInt(buffer, offset, protocolVersion); offset += 4;
        BinaryCodec.writeLong(buffer, offset, frame.getFrameIndex()); offset += 8;
        BinaryCodec.writeLong(buffer, offset, frame.getTimestamp()); offset += 8;
        BinaryCodec.writeString(buffer, offset, frame.getRoomId()); offset += BinaryCodec.stringSize(frame.getRoomId());

        // Characters
        BinaryCodec.writeInt(buffer, offset, chars.size()); offset += 4;
        for (var c : chars) {
            offset = writeChar(buffer, offset, c);
        }

        // Damage events
        BinaryCodec.writeInt(buffer, offset, dmgs.size()); offset += 4;
        for (var d : dmgs) {
            BinaryCodec.writeInt(buffer, offset, d.targetEntityId); offset += 4;
            BinaryCodec.writeFloat(buffer, offset, d.damage); offset += 4;
            buffer[offset++] = (byte) (d.isCritical ? 1 : 0);
            buffer[offset++] = (byte) d.element;
            offset = writeVector3(buffer, offset, d.position);
        }

        // VFX events
        BinaryCodec.writeInt(buffer, offset, vfxs.size()); offset += 4;
        for (var v : vfxs) {
            buffer[offset++] = (byte) v.vfxType;
            offset = writeVector3(buffer, offset, v.position);
            offset = writeQuaternion(buffer, offset, v.rotation);
            BinaryCodec.writeInt(buffer, offset, v.followEntityId); offset += 4;
            BinaryCodec.writeFloat(buffer, offset, v.duration); offset += 4;
        }

        // Trim to actual size
        byte[] result = new byte[offset];
        System.arraycopy(buffer, 0, result, 0, offset);
        return result;
    }

    private static int writeChar(byte[] buffer, int offset, BattleFrame.CharacterSnapshot c) {
        BinaryCodec.writeInt(buffer, offset, c.entityId); offset += 4;
        BinaryCodec.writeInt(buffer, offset, c.configId); offset += 4;
        buffer[offset++] = (byte) c.entityType;
        offset = writeVector3(buffer, offset, c.position);
        offset = writeQuaternion(buffer, offset, c.rotation);
        buffer[offset++] = (byte) c.state;

        // Stats
        var s = c.stats;
        BinaryCodec.writeInt(buffer, offset, (int) s.maxHealth); offset += 4;
        BinaryCodec.writeInt(buffer, offset, (int) s.currentHealth); offset += 4;
        BinaryCodec.writeInt(buffer, offset, (int) s.maxEnergy); offset += 4;
        BinaryCodec.writeInt(buffer, offset, (int) s.currentEnergy); offset += 4;
        BinaryCodec.writeInt(buffer, offset, (int) s.attackPower); offset += 4;
        BinaryCodec.writeInt(buffer, offset, (int) s.defense); offset += 4;
        BinaryCodec.writeFloat(buffer, offset, s.moveSpeed); offset += 4;
        BinaryCodec.writeFloat(buffer, offset, s.criticalRate); offset += 4;
        BinaryCodec.writeFloat(buffer, offset, s.criticalDamageMultiplier); offset += 4;
        BinaryCodec.writeInt(buffer, offset, (int) s.shieldAmount); offset += 4;

        // Buffs
        List<BattleFrame.BuffSnapshot> buffs = c.buffs;
        BinaryCodec.writeInt(buffer, offset, buffs.size()); offset += 4;
        for (var b : buffs) {
            BinaryCodec.writeInt(buffer, offset, b.buffId.hashCode()); offset += 4;
            BinaryCodec.writeString(buffer, offset, b.buffName); offset += BinaryCodec.stringSize(b.buffName);
            buffer[offset++] = (byte) b.buffType;
            BinaryCodec.writeFloat(buffer, offset, b.remainingTime); offset += 4;
            BinaryCodec.writeInt(buffer, offset, b.stacks); offset += 4;
            buffer[offset++] = (byte) b.element;
        }

        BinaryCodec.writeInt(buffer, offset, c.comboStep); offset += 4;
        buffer[offset++] = (byte) (c.isInvincible ? 1 : 0);
        BinaryCodec.writeInt(buffer, offset, c.targetEntityId); offset += 4;

        return offset;
    }

    private static int writeVector3(byte[] buffer, int offset, Vector3 v) {
        BinaryCodec.writeFloat(buffer, offset, v.x); offset += 4;
        BinaryCodec.writeFloat(buffer, offset, v.y); offset += 4;
        BinaryCodec.writeFloat(buffer, offset, v.z); offset += 4;
        return offset;
    }

    private static int writeQuaternion(byte[] buffer, int offset, Quaternion q) {
        BinaryCodec.writeFloat(buffer, offset, q.x); offset += 4;
        BinaryCodec.writeFloat(buffer, offset, q.y); offset += 4;
        BinaryCodec.writeFloat(buffer, offset, q.z); offset += 4;
        BinaryCodec.writeFloat(buffer, offset, q.w); offset += 4;
        return offset;
    }

    private static int estimateCharSize(BattleFrame.CharacterSnapshot c) {
        int size = 4 + 4 + 1 + 12 + 16 + 1; // entityId + configId + type + pos + rot + state
        size += 10 * 4; // stats (10 int/float fields)
        size += 4; // buff count
        for (var b : c.buffs) {
            size += 4 + BinaryCodec.stringSize(b.buffName) + 1 + 4 + 4 + 1;
        }
        size += 4 + 1 + 4; // comboStep + invincible + targetEntityId
        return size;
    }
}
