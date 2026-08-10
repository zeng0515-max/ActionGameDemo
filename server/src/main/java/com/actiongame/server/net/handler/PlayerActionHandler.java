package com.actiongame.server.net.handler;

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
 * 玩家操作处理器 - 接收客户端输入意图, 转发给 BattleRoom
 *
 * PlayerActionReq 二进制格式:
 *   [protocolVersion(4)] [playerEntityId(4)] [clientFrameIndex(8)] [actionCount(4)] [actions...]
 *   每个 action: [actionType(1)] [actionId(4)] [moveX(4)] [moveZ(4)] [skillId(4)] [targetElement(1)] [targetEntityId(4)] [timestamp(8)]
 *
 * PlayerActionResp 二进制格式:
 *   [protocolVersion(4)] [code(4)] [playerEntityId(4)] [clientFrameIndex(8)] [serverFrameIndex(8)] [accepted(1)] [rejectedActionType(4)] [rejectReason(str)]
 */
public class PlayerActionHandler implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(PlayerActionHandler.class);
    private static final int MAX_ACTIONS_PER_FRAME = 32;

    private final ConnectionManager connectionManager;
    private final com.actiongame.server.audit.AuditLoggerImpl auditLogger;

    public PlayerActionHandler(ConnectionManager connectionManager) {
        this(connectionManager, null);
    }

    public PlayerActionHandler(ConnectionManager connectionManager, com.actiongame.server.audit.AuditLoggerImpl auditLogger) {
        this.connectionManager = connectionManager;
        this.auditLogger = auditLogger;
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.PLAYER_ACTION_REQ;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, MessageWrapper wrapper, byte[] payload) throws Exception {
        GameSession session = connectionManager.getSession(ctx.channel());
        if (session == null || !session.isAuthenticated()) {
            sendResp(ctx, wrapper, -1, -1, 0, 0, false, 0, "Not authenticated");
            return;
        }

        int offset = 0;
        int protocolVersion = BinaryCodec.readInt(payload, offset); offset += 4;
        int playerEntityId = BinaryCodec.readInt(payload, offset); offset += 4;
        long clientFrameIndex = BinaryCodec.readLong(payload, offset); offset += 8;
        int actionCount = BinaryCodec.readInt(payload, offset); offset += 4;

        // 防止恶意客户端发送超大 actionCount 导致 DoS
        if (actionCount < 0 || actionCount > MAX_ACTIONS_PER_FRAME) {
            sendResp(ctx, wrapper, -3, playerEntityId, clientFrameIndex, 0, false, 0,
                "Invalid action count: " + actionCount);
            return;
        }

        // 查找玩家所在房间
        BattleRoom room = findRoomForPlayer(session.getPlayerId());
        if (room == null) {
            sendResp(ctx, wrapper, -2, playerEntityId, clientFrameIndex, 0, false, 0, "Room not found");
            return;
        }

        // 解析并提交所有操作
        int firstActionType = 0;
        for (int i = 0; i < actionCount; i++) {
            int actionType = payload[offset++] & 0xFF;
            if (i == 0) firstActionType = actionType;
            int actionId = BinaryCodec.readInt(payload, offset); offset += 4;
            float moveX = BinaryCodec.readFloat(payload, offset); offset += 4;
            float moveZ = BinaryCodec.readFloat(payload, offset); offset += 4;
            int skillId = BinaryCodec.readInt(payload, offset); offset += 4;
            offset += 1; // targetElement (byte)
            int targetEntityId = BinaryCodec.readInt(payload, offset); offset += 4;
            offset += 8; // timestamp (long)

            room.submitPlayerAction(playerEntityId, actionType, moveX, moveZ, skillId, targetEntityId);
        }

        if (actionCount > 0) {
            log.debug("Player {} submitted {} actions to room {} (frame {})",
                session.getPlayerId(), actionCount, room.getRoomId(), clientFrameIndex);
            if (auditLogger != null) auditLogger.logPlayerAction(session.getPlayerId(), firstActionType, room.getRoomId());
        }

        sendResp(ctx, wrapper, 0, playerEntityId, clientFrameIndex,
            (int) room.getCurrentFrameIndex(), true, 0, "");
    }

    private BattleRoom findRoomForPlayer(String playerId) {
        GameSession session = connectionManager.getSession(playerId);
        if (session == null) return null;
        String roomId = session.getRoomId();
        if (roomId == null) return null;
        return RoomManager.getInstance().getRoom(roomId);
    }

    private void sendResp(ChannelHandlerContext ctx, MessageWrapper wrapper,
                           int code, int entityId, long clientFrame, long serverFrame,
                           boolean accepted, int rejectedActionType, String reason) {
        int reasonSize = BinaryCodec.stringSize(reason);
        byte[] resp = new byte[4 + 4 + 4 + 8 + 8 + 1 + 4 + reasonSize];
        int offset = 0;
        BinaryCodec.writeInt(resp, offset, com.actiongame.server.constant.GameConstants.PROTOCOL_VERSION); offset += 4;
        BinaryCodec.writeInt(resp, offset, code); offset += 4;
        BinaryCodec.writeInt(resp, offset, entityId); offset += 4;
        BinaryCodec.writeLong(resp, offset, clientFrame); offset += 8;
        BinaryCodec.writeLong(resp, offset, serverFrame); offset += 8;
        resp[offset++] = (byte) (accepted ? 1 : 0);
        BinaryCodec.writeInt(resp, offset, rejectedActionType); offset += 4;
        BinaryCodec.writeString(resp, offset, reason);

        ctx.writeAndFlush(MessageHelper.wrap(MessageId.PLAYER_ACTION_RESP, wrapper.getSequenceId(), resp));
    }
}
