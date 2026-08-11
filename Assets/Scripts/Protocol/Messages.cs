using System;

namespace ActionGameDemo.Protocol
{
    // === 登录协议 ===

    public class LoginReq : IMessage
    {
        public int ProtocolVersion;
        public string Token;
        public string PlayerName;
        public int CharacterConfigId;

        public MessageId GetMessageId() => MessageId.LoginReq;

        public byte[] ToByteArray()
        {
            int tokenSize = BinarySerializer.StringSize(Token);
            int nameSize = BinarySerializer.StringSize(PlayerName);
            int size = 4 + tokenSize + nameSize + 4;
            byte[] buffer = new byte[size];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteString(buffer, offset, Token, 256); offset += tokenSize;
            BinarySerializer.WriteString(buffer, offset, PlayerName, 64); offset += nameSize;
            BinarySerializer.WriteInt(buffer, offset, CharacterConfigId);
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            Token = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(Token);
            PlayerName = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(PlayerName);
            CharacterConfigId = BinarySerializer.ReadInt(data, offset);
        }
    }

    public class LoginResp : IMessage
    {
        public int ProtocolVersion;
        public int Code;
        public string Message;
        public string PlayerId;
        public string SessionKey;
        public int ServerMinVersion;
        public int ServerMaxVersion;

        public MessageId GetMessageId() => MessageId.LoginResp;

        public byte[] ToByteArray()
        {
            int msgSize = BinarySerializer.StringSize(Message);
            int pidSize = BinarySerializer.StringSize(PlayerId);
            int keySize = BinarySerializer.StringSize(SessionKey);
            int size = 4 + 4 + msgSize + pidSize + keySize + 4 + 4;
            byte[] buffer = new byte[size];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, Code); offset += 4;
            BinarySerializer.WriteString(buffer, offset, Message, 256); offset += msgSize;
            BinarySerializer.WriteString(buffer, offset, PlayerId, 64); offset += pidSize;
            BinarySerializer.WriteString(buffer, offset, SessionKey, 128); offset += keySize;
            BinarySerializer.WriteInt(buffer, offset, ServerMinVersion); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, ServerMaxVersion);
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            Code = BinarySerializer.ReadInt(data, offset); offset += 4;
            Message = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(Message);
            PlayerId = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(PlayerId);
            SessionKey = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(SessionKey);
            ServerMinVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            ServerMaxVersion = BinarySerializer.ReadInt(data, offset);
        }
    }

    // === 心跳协议 ===

    public class HeartbeatReq : IMessage
    {
        public int ProtocolVersion;
        public long Timestamp;
        public string PlayerId;

        public MessageId GetMessageId() => MessageId.HeartbeatReq;

        public byte[] ToByteArray()
        {
            int pidSize = BinarySerializer.StringSize(PlayerId);
            int size = 4 + 8 + pidSize;
            byte[] buffer = new byte[size];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteLong(buffer, offset, Timestamp); offset += 8;
            BinarySerializer.WriteString(buffer, offset, PlayerId, 64);
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            Timestamp = BinarySerializer.ReadLong(data, offset); offset += 8;
            PlayerId = BinarySerializer.ReadString(data, offset);
        }
    }

    public class HeartbeatResp : IMessage
    {
        public int ProtocolVersion;
        public long ServerTimestamp;
        public long ClientTimestamp;
        public int Rtt;

        public MessageId GetMessageId() => MessageId.HeartbeatResp;

        public byte[] ToByteArray()
        {
            byte[] buffer = new byte[4 + 8 + 8 + 4];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteLong(buffer, offset, ServerTimestamp); offset += 8;
            BinarySerializer.WriteLong(buffer, offset, ClientTimestamp); offset += 8;
            BinarySerializer.WriteInt(buffer, offset, Rtt);
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            ServerTimestamp = BinarySerializer.ReadLong(data, offset); offset += 8;
            ClientTimestamp = BinarySerializer.ReadLong(data, offset); offset += 8;
            Rtt = BinarySerializer.ReadInt(data, offset);
        }
    }

    // === 房间协议 ===

    public class JoinRoomReq : IMessage
    {
        public int ProtocolVersion;
        public string PlayerId;
        public string RoomId;

        public MessageId GetMessageId() => MessageId.JoinRoomReq;

        public byte[] ToByteArray()
        {
            int pidSize = BinarySerializer.StringSize(PlayerId);
            int ridSize = BinarySerializer.StringSize(RoomId);
            byte[] buffer = new byte[4 + pidSize + ridSize];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteString(buffer, offset, PlayerId, 64); offset += pidSize;
            BinarySerializer.WriteString(buffer, offset, RoomId, 64);
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            PlayerId = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(PlayerId);
            RoomId = BinarySerializer.ReadString(data, offset);
        }
    }

    public class JoinRoomResp : IMessage
    {
        public int ProtocolVersion;
        public int Code;
        public string RoomId;
        public int PlayerEntityId;
        public int StartFrameIndex;
        public string RedirectNodeId;
        public string RedirectAddress;

        public MessageId GetMessageId() => MessageId.JoinRoomResp;

        public byte[] ToByteArray()
        {
            int ridSize = BinarySerializer.StringSize(RoomId);
            int nodeSize = BinarySerializer.StringSize(RedirectNodeId);
            int addrSize = BinarySerializer.StringSize(RedirectAddress);
            byte[] buffer = new byte[4 + 4 + ridSize + 4 + 4 + nodeSize + addrSize];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, Code); offset += 4;
            BinarySerializer.WriteString(buffer, offset, RoomId, 64); offset += ridSize;
            BinarySerializer.WriteInt(buffer, offset, PlayerEntityId); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, StartFrameIndex); offset += 4;
            BinarySerializer.WriteString(buffer, offset, RedirectNodeId, 64); offset += nodeSize;
            BinarySerializer.WriteString(buffer, offset, RedirectAddress, 128);
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            Code = BinarySerializer.ReadInt(data, offset); offset += 4;
            RoomId = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(RoomId);
            PlayerEntityId = BinarySerializer.ReadInt(data, offset); offset += 4;
            StartFrameIndex = BinarySerializer.ReadInt(data, offset);
            RedirectNodeId = "";
            RedirectAddress = "";
            if (offset < data.Length)
            {
                RedirectNodeId = BinarySerializer.ReadString(data, offset);
                offset += BinarySerializer.StringSize(RedirectNodeId);
            }
            if (offset < data.Length)
            {
                RedirectAddress = BinarySerializer.ReadString(data, offset);
            }
        }
    }

    // === 战斗操作协议 ===

    public class PlayerActionReq : IMessage
    {
        public int ProtocolVersion;
        public int PlayerEntityId;
        public long ClientFrameIndex;
        public PlayerActionData[] Actions;

        public MessageId GetMessageId() => MessageId.PlayerActionReq;

        public byte[] ToByteArray()
        {
            int actionCount = Actions?.Length ?? 0;
            int size = 4 + 4 + 8 + 4;
            for (int i = 0; i < actionCount; i++)
                size += Actions[i].SerializedSize;

            byte[] buffer = new byte[size];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, PlayerEntityId); offset += 4;
            BinarySerializer.WriteLong(buffer, offset, ClientFrameIndex); offset += 8;
            BinarySerializer.WriteInt(buffer, offset, actionCount); offset += 4;
            for (int i = 0; i < actionCount; i++)
            {
                Actions[i].Serialize(buffer, ref offset);
            }
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            PlayerEntityId = BinarySerializer.ReadInt(data, offset); offset += 4;
            ClientFrameIndex = BinarySerializer.ReadLong(data, offset); offset += 8;
            int count = BinarySerializer.ReadInt(data, offset); offset += 4;
            Actions = new PlayerActionData[count];
            for (int i = 0; i < count; i++)
            {
                Actions[i].Deserialize(data, ref offset);
            }
        }
    }

    public struct PlayerActionData
    {
        public ActionType ActionType;
        public int ActionId;
        public float MoveX;
        public float MoveZ;
        public int SkillId;
        public ElementType TargetElement;
        public int TargetEntityId;
        public long Timestamp;

        public int SerializedSize => 1 + 4 + 4 + 4 + 4 + 1 + 4 + 8;

        public void Serialize(byte[] buffer, ref int offset)
        {
            buffer[offset++] = (byte)ActionType;
            BinarySerializer.WriteInt(buffer, offset, ActionId); offset += 4;
            BinarySerializer.WriteFloat(buffer, offset, MoveX); offset += 4;
            BinarySerializer.WriteFloat(buffer, offset, MoveZ); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, SkillId); offset += 4;
            buffer[offset++] = (byte)TargetElement;
            BinarySerializer.WriteInt(buffer, offset, TargetEntityId); offset += 4;
            BinarySerializer.WriteLong(buffer, offset, Timestamp); offset += 8;
        }

        public void Deserialize(byte[] buffer, ref int offset)
        {
            ActionType = (ActionType)buffer[offset++];
            ActionId = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            MoveX = BinarySerializer.ReadFloat(buffer, offset); offset += 4;
            MoveZ = BinarySerializer.ReadFloat(buffer, offset); offset += 4;
            SkillId = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            TargetElement = (ElementType)buffer[offset++];
            TargetEntityId = BinarySerializer.ReadInt(buffer, offset); offset += 4;
            Timestamp = BinarySerializer.ReadLong(buffer, offset); offset += 8;
        }
    }

    public class PlayerActionResp : IMessage
    {
        public int ProtocolVersion;
        public int Code;
        public int PlayerEntityId;
        public long ClientFrameIndex;
        public long ServerFrameIndex;
        public bool Accepted;
        public int RejectedActionType;
        public string RejectReason;

        public MessageId GetMessageId() => MessageId.PlayerActionResp;

        public byte[] ToByteArray()
        {
            int reasonSize = BinarySerializer.StringSize(RejectReason);
            byte[] buffer = new byte[4 + 4 + 4 + 8 + 8 + 1 + 4 + reasonSize];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, Code); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, PlayerEntityId); offset += 4;
            BinarySerializer.WriteLong(buffer, offset, ClientFrameIndex); offset += 8;
            BinarySerializer.WriteLong(buffer, offset, ServerFrameIndex); offset += 8;
            buffer[offset++] = (byte)(Accepted ? 1 : 0);
            BinarySerializer.WriteInt(buffer, offset, RejectedActionType); offset += 4;
            BinarySerializer.WriteString(buffer, offset, RejectReason, 256); offset += reasonSize;
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            Code = BinarySerializer.ReadInt(data, offset); offset += 4;
            PlayerEntityId = BinarySerializer.ReadInt(data, offset); offset += 4;
            ClientFrameIndex = BinarySerializer.ReadLong(data, offset); offset += 8;
            ServerFrameIndex = BinarySerializer.ReadLong(data, offset); offset += 8;
            Accepted = data[offset++] != 0;
            RejectedActionType = BinarySerializer.ReadInt(data, offset); offset += 4;
            RejectReason = BinarySerializer.ReadString(data, offset);
        }
    }

    // === 帧同步协议 ===

    public class BattleFrameNotify : IMessage
    {
        public int ProtocolVersion;
        public long FrameIndex;
        public long Timestamp;
        public string RoomId;
        public CharacterSnapshot[] Characters;
        public DamageNumberEventData[] DamageEvents;
        public VFXEventData[] VfxEvents;

        public MessageId GetMessageId() => MessageId.BattleFrameNotify;

        public byte[] ToByteArray()
        {
            int ridSize = BinarySerializer.StringSize(RoomId);
            int charCount = Characters?.Length ?? 0;
            int dmgCount = DamageEvents?.Length ?? 0;
            int vfxCount = VfxEvents?.Length ?? 0;

            // 估算总大小: header(4+8+8+rid+4) + chars(256 each) + dmgs(64 each) + vfx(128 each) + padding
            int estimatedSize = 4 + 8 + 8 + ridSize + 4 + charCount * 256 + dmgCount * 64 + vfxCount * 128 + 256;
            byte[] buffer = new byte[estimatedSize];
            int offset = 0;

            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteLong(buffer, offset, FrameIndex); offset += 8;
            BinarySerializer.WriteLong(buffer, offset, Timestamp); offset += 8;
            BinarySerializer.WriteString(buffer, offset, RoomId, 64); offset += ridSize;

            BinarySerializer.WriteInt(buffer, offset, charCount); offset += 4;
            for (int i = 0; i < charCount; i++)
                Characters[i].Serialize(buffer, ref offset);

            BinarySerializer.WriteInt(buffer, offset, dmgCount); offset += 4;
            for (int i = 0; i < dmgCount; i++)
                DamageEvents[i].Serialize(buffer, ref offset);

            BinarySerializer.WriteInt(buffer, offset, vfxCount); offset += 4;
            for (int i = 0; i < vfxCount; i++)
                VfxEvents[i].Serialize(buffer, ref offset);

            // 截取实际使用的部分
            byte[] result = new byte[offset];
            System.Array.Copy(buffer, 0, result, 0, offset);
            return result;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            FrameIndex = BinarySerializer.ReadLong(data, offset); offset += 8;
            Timestamp = BinarySerializer.ReadLong(data, offset); offset += 8;
            RoomId = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(RoomId);

            int charCount = BinarySerializer.ReadInt(data, offset); offset += 4;
            Characters = new CharacterSnapshot[charCount];
            for (int i = 0; i < charCount; i++)
                Characters[i].Deserialize(data, ref offset);

            int dmgCount = BinarySerializer.ReadInt(data, offset); offset += 4;
            DamageEvents = new DamageNumberEventData[dmgCount];
            for (int i = 0; i < dmgCount; i++)
                DamageEvents[i].Deserialize(data, ref offset);

            int vfxCount = BinarySerializer.ReadInt(data, offset); offset += 4;
            VfxEvents = new VFXEventData[vfxCount];
            for (int i = 0; i < vfxCount; i++)
                VfxEvents[i].Deserialize(data, ref offset);
        }
    }

    public class BattleStartNotify : IMessage
    {
        public int ProtocolVersion;
        public string RoomId;
        public long StartTime;
        public long StartFrameIndex;

        public MessageId GetMessageId() => MessageId.BattleStartNotify;

        public byte[] ToByteArray()
        {
            int ridSize = BinarySerializer.StringSize(RoomId);
            byte[] buffer = new byte[4 + ridSize + 8 + 8];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteString(buffer, offset, RoomId, 64); offset += ridSize;
            BinarySerializer.WriteLong(buffer, offset, StartTime); offset += 8;
            BinarySerializer.WriteLong(buffer, offset, StartFrameIndex);
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            RoomId = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(RoomId);
            StartTime = BinarySerializer.ReadLong(data, offset); offset += 8;
            StartFrameIndex = BinarySerializer.ReadLong(data, offset);
        }
    }

    public class BattleEndNotify : IMessage
    {
        public int ProtocolVersion;
        public string RoomId;
        public int Result;
        public long EndTime;
        public long TotalFrames;
        public int KillCount;
        public float TotalDamage;

        public MessageId GetMessageId() => MessageId.BattleEndNotify;

        public byte[] ToByteArray()
        {
            int ridSize = BinarySerializer.StringSize(RoomId);
            byte[] buffer = new byte[4 + ridSize + 4 + 8 + 8 + 4 + 4];
            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteString(buffer, offset, RoomId, 64); offset += ridSize;
            BinarySerializer.WriteInt(buffer, offset, Result); offset += 4;
            BinarySerializer.WriteLong(buffer, offset, EndTime); offset += 8;
            BinarySerializer.WriteLong(buffer, offset, TotalFrames); offset += 8;
            BinarySerializer.WriteInt(buffer, offset, KillCount); offset += 4;
            BinarySerializer.WriteFloat(buffer, offset, TotalDamage);
            return buffer;
        }

        public void FromByteArray(byte[] data)
        {
            int offset = 0;
            ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            RoomId = BinarySerializer.ReadString(data, offset); offset += BinarySerializer.StringSize(RoomId);
            Result = BinarySerializer.ReadInt(data, offset); offset += 4;
            EndTime = BinarySerializer.ReadLong(data, offset); offset += 8;
            TotalFrames = BinarySerializer.ReadLong(data, offset); offset += 8;
            KillCount = BinarySerializer.ReadInt(data, offset); offset += 4;
            TotalDamage = BinarySerializer.ReadFloat(data, offset);
        }
    }
}
