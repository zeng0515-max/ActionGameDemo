using System;

namespace ActionGameDemo.Protocol
{
    /// <summary>
    /// 三维向量 (Y-Up, 与Unity一致)
    /// </summary>
    public struct Vector3Proto
    {
        public float X;
        public float Y;
        public float Z;

        public Vector3Proto(float x, float y, float z) { X = x; Y = y; Z = z; }

        public int SerializedSize => 12;

        public void Serialize(byte[] buffer, int offset)
        {
            BinarySerializer.WriteFloat(buffer, offset, X);
            BinarySerializer.WriteFloat(buffer, offset + 4, Y);
            BinarySerializer.WriteFloat(buffer, offset + 8, Z);
        }

        public void Deserialize(byte[] buffer, int offset)
        {
            X = BinarySerializer.ReadFloat(buffer, offset);
            Y = BinarySerializer.ReadFloat(buffer, offset + 4);
            Z = BinarySerializer.ReadFloat(buffer, offset + 8);
        }
    }

    /// <summary>
    /// 四元数旋转
    /// </summary>
    public struct QuaternionProto
    {
        public float X;
        public float Y;
        public float Z;
        public float W;

        public int SerializedSize => 16;

        public void Serialize(byte[] buffer, int offset)
        {
            BinarySerializer.WriteFloat(buffer, offset, X);
            BinarySerializer.WriteFloat(buffer, offset + 4, Y);
            BinarySerializer.WriteFloat(buffer, offset + 8, Z);
            BinarySerializer.WriteFloat(buffer, offset + 12, W);
        }

        public void Deserialize(byte[] buffer, int offset)
        {
            X = BinarySerializer.ReadFloat(buffer, offset);
            Y = BinarySerializer.ReadFloat(buffer, offset + 4);
            Z = BinarySerializer.ReadFloat(buffer, offset + 8);
            W = BinarySerializer.ReadFloat(buffer, offset + 12);
        }
    }

    /// <summary>
    /// 角色属性快照 (只读下发, 客户端禁止修改)
    /// </summary>
    public struct CharacterStatsSnapshot
    {
        public int MaxHealth;
        public int CurrentHealth;
        public int MaxEnergy;
        public int CurrentEnergy;
        public int AttackPower;
        public int Defense;
        public float MoveSpeed;
        public float CriticalRate;
        public float CriticalDamageMultiplier;
        public int ShieldAmount;

        public int SerializedSize => 40;

        public void Serialize(byte[] buffer, int offset)
        {
            BinarySerializer.WriteInt(buffer, offset, MaxHealth);
            BinarySerializer.WriteInt(buffer, offset + 4, CurrentHealth);
            BinarySerializer.WriteInt(buffer, offset + 8, MaxEnergy);
            BinarySerializer.WriteInt(buffer, offset + 12, CurrentEnergy);
            BinarySerializer.WriteInt(buffer, offset + 16, AttackPower);
            BinarySerializer.WriteInt(buffer, offset + 20, Defense);
            BinarySerializer.WriteFloat(buffer, offset + 24, MoveSpeed);
            BinarySerializer.WriteFloat(buffer, offset + 28, CriticalRate);
            BinarySerializer.WriteFloat(buffer, offset + 32, CriticalDamageMultiplier);
            BinarySerializer.WriteInt(buffer, offset + 36, ShieldAmount);
        }

        public void Deserialize(byte[] buffer, int offset)
        {
            MaxHealth = BinarySerializer.ReadInt(buffer, offset);
            CurrentHealth = BinarySerializer.ReadInt(buffer, offset + 4);
            MaxEnergy = BinarySerializer.ReadInt(buffer, offset + 8);
            CurrentEnergy = BinarySerializer.ReadInt(buffer, offset + 12);
            AttackPower = BinarySerializer.ReadInt(buffer, offset + 16);
            Defense = BinarySerializer.ReadInt(buffer, offset + 20);
            MoveSpeed = BinarySerializer.ReadFloat(buffer, offset + 24);
            CriticalRate = BinarySerializer.ReadFloat(buffer, offset + 28);
            CriticalDamageMultiplier = BinarySerializer.ReadFloat(buffer, offset + 32);
            ShieldAmount = BinarySerializer.ReadInt(buffer, offset + 36);
        }
    }

    /// <summary>
    /// Buff快照 (客户端仅显示, 不计算效果)
    /// </summary>
    public struct BuffSnapshot
    {
        public int BuffId;
        public string BuffName;
        public BuffType BuffType;
        public float RemainingTime;
        public int Stacks;
        public ElementType Element;

        public void Serialize(byte[] buffer, ref int offset)
        {
            BinarySerializer.WriteInt(buffer, offset, BuffId); offset += 4;
            BinarySerializer.WriteString(buffer, offset, BuffName, 64); offset += BinarySerializer.StringSize(BuffName);
            buffer[offset++] = (byte)BuffType;
            BinarySerializer.WriteFloat(buffer, offset, RemainingTime); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, Stacks); offset += 4;
            buffer[offset++] = (byte)Element;
        }

        public void Deserialize(byte[] buffer, ref int offset)
        {
            BuffId = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            BuffName = BinarySerializer.ReadString(buffer, offset); offset += BinarySerializer.StringSize(BuffName);
            BuffType = (BuffType)buffer[offset++];
            RemainingTime = BinarySerializer.ReadFloat(buffer, offset); offset += 4;
            Stacks = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            Element = (ElementType)buffer[offset++];
        }
    }

    /// <summary>
    /// 伤害飘字事件 (数值由服务端计算)
    /// </summary>
    public struct DamageNumberEventData
    {
        public int TargetEntityId;
        public float Damage;
        public bool IsCritical;
        public ElementType Element;
        public Vector3Proto Position;

        public void Serialize(byte[] buffer, ref int offset)
        {
            BinarySerializer.WriteInt(buffer, offset, TargetEntityId); offset += 4;
            BinarySerializer.WriteFloat(buffer, offset, Damage); offset += 4;
            buffer[offset++] = (byte)(IsCritical ? 1 : 0);
            buffer[offset++] = (byte)Element;
            Position.Serialize(buffer, offset); offset += Position.SerializedSize;
        }

        public void Deserialize(byte[] buffer, ref int offset)
        {
            TargetEntityId = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            Damage = BinarySerializer.ReadFloat(buffer, offset); offset += 4;
            IsCritical = buffer[offset++] != 0;
            Element = (ElementType)buffer[offset++];
            Position.Deserialize(buffer, offset); offset += Position.SerializedSize;
        }
    }

    /// <summary>
    /// 特效事件 (位置由服务端计算)
    /// </summary>
    public struct VFXEventData
    {
        public VFXType VfxType;
        public Vector3Proto Position;
        public QuaternionProto Rotation;
        public int FollowEntityId;
        public float Duration;

        public void Serialize(byte[] buffer, ref int offset)
        {
            buffer[offset++] = (byte)VfxType;
            Position.Serialize(buffer, offset); offset += Position.SerializedSize;
            Rotation.Serialize(buffer, offset); offset += Rotation.SerializedSize;
            BinarySerializer.WriteInt(buffer, offset, FollowEntityId); offset += 4;
            BinarySerializer.WriteFloat(buffer, offset, Duration); offset += 4;
        }

        public void Deserialize(byte[] buffer, ref int offset)
        {
            VfxType = (VFXType)buffer[offset++];
            Position.Deserialize(buffer, offset); offset += Position.SerializedSize;
            Rotation.Deserialize(buffer, offset); offset += Rotation.SerializedSize;
            FollowEntityId = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            Duration = BinarySerializer.ReadFloat(buffer, offset); offset += 4;
        }
    }

    /// <summary>
    /// 角色完整快照 (客户端只读, 从帧快照读取)
    /// </summary>
    public struct CharacterSnapshot
    {
        public int EntityId;
        public int ConfigId;
        public int EntityType; // 0=player, 1=monster, 2=boss
        public Vector3Proto Position;
        public QuaternionProto Rotation;
        public CharacterState State;
        public CharacterStatsSnapshot Stats;
        public BuffSnapshot[] Buffs;
        public int ComboStep;
        public bool IsInvincible;
        public int TargetEntityId;

        public void Serialize(byte[] buffer, ref int offset)
        {
            BinarySerializer.WriteInt(buffer, offset, EntityId); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, ConfigId); offset += 4;
            buffer[offset++] = (byte)EntityType;
            Position.Serialize(buffer, offset); offset += Position.SerializedSize;
            Rotation.Serialize(buffer, offset); offset += Rotation.SerializedSize;
            buffer[offset++] = (byte)State;
            Stats.Serialize(buffer, offset); offset += Stats.SerializedSize;
            int buffCount = Buffs?.Length ?? 0;
            BinarySerializer.WriteInt(buffer, offset, buffCount); offset += 4;
            for (int i = 0; i < buffCount; i++)
            {
                Buffs[i].Serialize(buffer, ref offset);
            }
            BinarySerializer.WriteInt(buffer, offset, ComboStep); offset += 4;
            buffer[offset++] = (byte)(IsInvincible ? 1 : 0);
            BinarySerializer.WriteInt(buffer, offset, TargetEntityId); offset += 4;
        }

        public void Deserialize(byte[] buffer, ref int offset)
        {
            EntityId = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            ConfigId = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            EntityType = buffer[offset++];
            Position.Deserialize(buffer, offset); offset += Position.SerializedSize;
            Rotation.Deserialize(buffer, offset); offset += Rotation.SerializedSize;
            State = (CharacterState)buffer[offset++];
            Stats.Deserialize(buffer, offset); offset += Stats.SerializedSize;
            int buffCount = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            Buffs = new BuffSnapshot[buffCount];
            for (int i = 0; i < buffCount; i++)
            {
                Buffs[i].Deserialize(buffer, ref offset);
            }
            ComboStep = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            IsInvincible = buffer[offset++] != 0;
            TargetEntityId = BinarySerializer.ReadInt(buffer, offset); offset += 4;
        }
    }
}
