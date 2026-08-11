using System;
using System.Collections.Generic;

namespace ActionGameDemo.Protocol
{
    /// <summary>
    /// 所有协议消息的基接口
    /// </summary>
    public interface IMessage
    {
        /// <summary>消息ID</summary>
        MessageId GetMessageId();

        /// <summary>序列化为字节数组</summary>
        byte[] ToByteArray();

        /// <summary>从字节数组反序列化</summary>
        void FromByteArray(byte[] data);
    }

    /// <summary>
    /// 消息ID枚举 (与服务端proto定义一致)
    /// </summary>
    public enum MessageId
    {
        Unknown = 0,

        // 登录 1001-1099
        LoginReq = 1001,
        LoginResp = 1002,

        // 房间 1101-1199
        JoinRoomReq = 1101,
        JoinRoomResp = 1102,
        LeaveRoomReq = 1103,
        PlayerJoinNotify = 1104,
        PlayerLeaveNotify = 1105,

        // 战斗 1201-1299
        PlayerActionReq = 1201,
        PlayerActionResp = 1202,
        BattleFrameNotify = 1203,
        DeltaFrameNotify = 1204,
        BattleStartNotify = 1205,
        BattleEndNotify = 1206,

        // 系统 2001-2099
        HeartbeatReq = 2001,
        HeartbeatResp = 2002,

        // 回放 3001-3099
        ReplayReq = 3001,
        ReplayResp = 3002,
    }

    /// <summary>
    /// 元素类型
    /// </summary>
    public enum ElementType
    {
        None = 0,
        Fire = 1,
        Ice = 2,
        Lightning = 3,
        Water = 4,
    }

    /// <summary>
    /// 角色状态
    /// </summary>
    public enum CharacterState
    {
        Idle = 0,
        Move = 1,
        Jump = 2,
        Attack = 3,
        Skill = 4,
        Ultimate = 5,
        Dodge = 6,
        Hurt = 7,
        Dead = 8,
    }

    /// <summary>
    /// 实体类型
    /// </summary>
    public enum EntityType
    {
        Player = 0,
        Monster = 1,
        Boss = 2,
        NPC = 3,
    }

    /// <summary>
    /// Buff类型
    /// </summary>
    public enum BuffType
    {
        None = 0,
        Attribute = 1,
        Dot = 2,
        Control = 3,
        Shield = 4,
    }

    /// <summary>
    /// VFX类型
    /// </summary>
    public enum VFXType
    {
        None = 0,
        Slash = 1,
        HitImpact = 2,
        CriticalHit = 3,
        Dodge = 4,
        Death = 5,
        Heal = 6,
        SkillCast = 7,
        UltimateCast = 8,
        UltimateCastFire = 9,
        BuffAppear = 10,
        Shield = 11,
        FireBurn = 12,
        IceFreeze = 13,
        LightningShock = 14,
        Poison = 15,
    }

    /// <summary>
    /// 操作类型
    /// </summary>
    public enum ActionType
    {
        None = 0,
        Move = 1,
        Jump = 2,
        Attack = 3,
        Skill = 4,
        Ultimate = 5,
        Dodge = 6,
        SwitchElement = 7,
    }
}
