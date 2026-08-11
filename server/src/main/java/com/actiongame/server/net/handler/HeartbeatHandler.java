package com.actiongame.server.net.handler;

import com.actiongame.server.net.util.BinaryCodec;
import com.actiongame.server.net.util.MessageHelper;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 心跳处理器 - 处理心跳请求并返回RTT
 *
 * HeartbeatReq: [protocolVersion(4)] [timestamp(8)] [playerId(str)]
 * HeartbeatResp: [protocolVersion(4)] [serverTimestamp(8)] [clientTimestamp(8)] [rtt(4)]
 */
public class HeartbeatHandler implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatHandler.class);

    @Override
    public MessageId getMessageId() {
        return MessageId.HEARTBEAT_REQ;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, MessageWrapper wrapper, byte[] payload) throws Exception {
        int offset = 0;
        int protocolVersion = BinaryCodec.readInt(payload, offset); offset += 4;
        long clientTimestamp = BinaryCodec.readLong(payload, offset); offset += 8;
        String playerId = BinaryCodec.readString(payload, offset);

        long serverTime = System.currentTimeMillis();
        int rtt = (int) (serverTime - clientTimestamp);

        // 构建HeartbeatResp
        byte[] respPayload = new byte[4 + 8 + 8 + 4];
        offset = 0;
        BinaryCodec.writeInt(respPayload, offset, protocolVersion); offset += 4;
        BinaryCodec.writeLong(respPayload, offset, serverTime); offset += 8;
        BinaryCodec.writeLong(respPayload, offset, clientTimestamp); offset += 8;
        BinaryCodec.writeInt(respPayload, offset, rtt);

        MessageWrapper respWrapper = MessageHelper.wrap(
                MessageId.HEARTBEAT_RESP,
                wrapper.getSequenceId(),
                respPayload
        );

        ctx.writeAndFlush(respWrapper);

        log.debug("Heartbeat: playerId={}, rtt={}ms", playerId, rtt);
    }
}
