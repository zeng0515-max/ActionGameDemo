package com.actiongame.server.net.handler;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.PlayerCharacter;
import com.actiongame.server.matchmaking.GlobalMatchmaker;
import com.actiongame.server.matchmaking.Matchmaker;
import com.actiongame.server.net.session.ConnectionManager;
import com.actiongame.server.net.session.GameSession;
import com.actiongame.server.net.util.BinaryCodec;
import com.actiongame.server.net.util.MessageHelper;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import com.actiongame.server.room.BattleRoom;
import com.actiongame.server.room.RoomManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 加入房间处理器
 *
 * JoinRoomReq: [protocolVersion(4)] [playerId(str)] [roomId(str)]
 * JoinRoomResp: [protocolVersion(4)] [code(4)] [roomId(str)] [playerEntityId(4)] [startFrameIndex(4)]
 */
public class JoinRoomHandler implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(JoinRoomHandler.class);
    private static final int CODE_ROOM_REDIRECT = -3;
    private static final int CODE_NODE_DRAINING = -4;

    private final ConnectionManager connectionManager;
    private final com.actiongame.server.audit.AuditLoggerImpl auditLogger;
    private final RoomManager roomManager;
    private final Matchmaker matchmaker;

    public JoinRoomHandler(ConnectionManager connectionManager) {
        this(connectionManager, null, RoomManager.getInstance());
    }

    public JoinRoomHandler(ConnectionManager connectionManager, com.actiongame.server.audit.AuditLoggerImpl auditLogger) {
        this(connectionManager, auditLogger, RoomManager.getInstance());
    }

    public JoinRoomHandler(ConnectionManager connectionManager,
                           com.actiongame.server.audit.AuditLoggerImpl auditLogger,
                           RoomManager roomManager) {
        this(connectionManager, auditLogger, roomManager,
            new GlobalMatchmaker(roomManager, roomManager.getNodeRegistry(), roomManager.getRoomRouter()));
    }

    public JoinRoomHandler(ConnectionManager connectionManager,
                           com.actiongame.server.audit.AuditLoggerImpl auditLogger,
                           RoomManager roomManager,
                           Matchmaker matchmaker) {
        this.connectionManager = connectionManager;
        this.auditLogger = auditLogger;
        this.roomManager = roomManager;
        this.matchmaker = matchmaker;
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.JOIN_ROOM_REQ;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, MessageWrapper wrapper, byte[] payload) throws Exception {
        GameSession session = connectionManager.getSession(ctx.channel());
        if (session == null || !session.isAuthenticated()) {
            sendJoinRoomResp(ctx, wrapper, -1, "", -1, 0, "", "");
            return;
        }

        int offset = 0;
        int protocolVersion = BinaryCodec.readInt(payload, offset); offset += 4;
        String clientPlayerId = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(clientPlayerId);
        String roomId = BinaryCodec.readString(payload, offset);

        // 安全: 使用 session 的 playerId, 不信任客户端传入
        String playerId = session.getPlayerId();
        RoomManager roomManager = this.roomManager;

        if (roomId == null || roomId.isEmpty()) {
            if (roomManager.isDraining()) {
                sendJoinRoomResp(ctx, wrapper, CODE_NODE_DRAINING, "", -1, 0, "", "");
                return;
            }
            roomId = matchmaker.allocateRoom(playerId);
        }

        // 获取或创建房间
        String roomOwner = roomManager.getRoomOwner(roomId);
        if (roomOwner != null && !roomOwner.equals(roomManager.getNodeId())) {
            if (!roomManager.releaseStaleRoom(roomId)) {
                sendJoinRoomResp(ctx, wrapper, CODE_ROOM_REDIRECT, roomId, -1, 0,
                    roomOwner, roomManager.resolveNodeAddress(roomOwner));
                return;
            }
            roomOwner = roomManager.getRoomOwner(roomId);
        }

        if (roomOwner == null && !roomManager.isDraining()) {
            roomManager.restoreRoomIfAvailable(roomId);
            roomOwner = roomManager.getRoomOwner(roomId);
            if (roomOwner != null && !roomOwner.equals(roomManager.getNodeId())) {
                sendJoinRoomResp(ctx, wrapper, CODE_ROOM_REDIRECT, roomId, -1, 0,
                    roomOwner, roomManager.resolveNodeAddress(roomOwner));
                return;
            }
        }

        if (roomManager.isDraining() && roomManager.getRoom(roomId) == null) {
            sendJoinRoomResp(ctx, wrapper, CODE_NODE_DRAINING, roomId, -1, 0, "", "");
            return;
        }

        BattleRoom room;
        try {
            room = roomManager.getOrCreateRoom(roomId);
        } catch (IllegalStateException e) {
            String owner = roomManager.getRoomOwner(roomId);
            log.warn("Room {} is owned by another node: {}", roomId, owner);
            sendJoinRoomResp(ctx, wrapper, CODE_ROOM_REDIRECT, roomId, -1, 0,
                owner, owner == null ? "" : roomManager.resolveNodeAddress(owner));
            return;
        }

        // 创建玩家角色
        CharacterStats stats = new CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        PlayerCharacter character = new PlayerCharacter(0, 1, stats, playerId);

        int entityId = room.addPlayer(character, session);
        if (entityId < 0) {
            sendJoinRoomResp(ctx, wrapper, -2, roomId, -1, 0, "", "");
            return;
        }

        log.info("Player {} joined room {}, entityId={}", playerId, roomId, entityId);
        if (auditLogger != null) auditLogger.logPlayerJoinRoom(playerId, roomId, entityId);

        session.setRoomId(roomId);
        sendJoinRoomResp(ctx, wrapper, 0, roomId, entityId, (int) room.getCurrentFrameIndex(), "", "");

        // 如果房间还没开始战斗, 生成怪物并自动开始
        if (room.getStatus() == com.actiongame.server.room.RoomStatus.WAITING) {
            spawnMonstersAndStart(room);
        }
    }

    /**
     * 生成怪物并开始战斗
     */
    private void spawnMonstersAndStart(BattleRoom room) {
        // 生成3个普通怪 + 1个Boss
        com.actiongame.server.config.MonsterConfig normalConfig = new com.actiongame.server.config.MonsterConfig();
        normalConfig.setPatrolRadius(8f);  // 更大巡逻范围
        normalConfig.setMoveSpeed(3f);
        for (int i = 0; i < 3; i++) {
            room.spawnMonster(normalConfig, false);
        }

        com.actiongame.server.config.MonsterConfig bossConfig = new com.actiongame.server.config.MonsterConfig();
        bossConfig.setMaxHealth(200f);
        bossConfig.setAttackPower(10f);
        bossConfig.setDefense(5f);
        bossConfig.setMoveSpeed(2f);
        room.spawnMonster(bossConfig, true);

        // 发送 BattleStartNotify
        for (var rp : room.getPlayers()) {
            var session = rp.getSession();
            if (session != null && session.isActive()) {
                byte[] startPayload = new byte[4 + BinaryCodec.stringSize(room.getRoomId()) + 8 + 8];
                int off = 0;
                BinaryCodec.writeInt(startPayload, off, GameConstants.PROTOCOL_VERSION); off += 4;
                BinaryCodec.writeString(startPayload, off, room.getRoomId()); off += BinaryCodec.stringSize(room.getRoomId());
                BinaryCodec.writeLong(startPayload, off, System.currentTimeMillis()); off += 8;
                BinaryCodec.writeLong(startPayload, off, 0);

                var wrapper = com.actiongame.server.net.util.MessageHelper.wrap(
                    com.actiongame.server.proto.MessageWrapperProto.MessageId.BATTLE_START_NOTIFY,
                    session.nextSequenceId(),
                    startPayload
                );
                session.send(wrapper);
            }
        }

        room.startBattle();
        log.info("Room {} battle started with {} monsters", room.getRoomId(), room.getMonsters().size());
        if (auditLogger != null) auditLogger.logBattleStart(room.getRoomId(), room.getPlayerCount());
    }

    private void sendJoinRoomResp(ChannelHandlerContext ctx, MessageWrapper wrapper,
                                   int code, String roomId, int entityId, int startFrameIndex,
                                   String redirectNodeId, String redirectAddress) {
        int respSize = 4 + 4 + BinaryCodec.stringSize(roomId) + 4 + 4
            + BinaryCodec.stringSize(redirectNodeId) + BinaryCodec.stringSize(redirectAddress);
        byte[] respPayload = new byte[respSize];
        int offset = 0;
        BinaryCodec.writeInt(respPayload, offset, GameConstants.PROTOCOL_VERSION); offset += 4;
        BinaryCodec.writeInt(respPayload, offset, code); offset += 4;
        BinaryCodec.writeString(respPayload, offset, roomId); offset += BinaryCodec.stringSize(roomId);
        BinaryCodec.writeInt(respPayload, offset, entityId); offset += 4;
        BinaryCodec.writeInt(respPayload, offset, startFrameIndex); offset += 4;
        BinaryCodec.writeString(respPayload, offset, redirectNodeId); offset += BinaryCodec.stringSize(redirectNodeId);
        BinaryCodec.writeString(respPayload, offset, redirectAddress);

        MessageWrapper respWrapper = MessageHelper.wrap(
            MessageId.JOIN_ROOM_RESP,
            wrapper.getSequenceId(),
            respPayload
        );
        ctx.writeAndFlush(respWrapper);
    }
}
