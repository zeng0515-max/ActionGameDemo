package com.actiongame.server.net.codec;

import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * 消息解码器 - 从BinaryWebSocketFrame解析消息
 *
 * 支持客户端自定义二进制格式:
 * [消息ID(4,BE)] [协议版本(4,BE)] [序列号(8,BE)] [Payload长度(4,BE)] [Payload]
 *
 * 转换为Protobuf MessageWrapper供内部使用
 */
public class MessageDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(MessageDecoder.class);
    private static final int MAX_PAYLOAD_SIZE = 256 * 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame frame, List<Object> out) throws Exception {
        ByteBuf buf = frame.content();
        if (buf.readableBytes() < 20) {
            log.warn("Message too short: {} bytes", buf.readableBytes());
            return;
        }

        buf = buf.order(ByteOrder.BIG_ENDIAN);
        int messageIdValue = buf.readInt();
        int protocolVersion = buf.readInt();
        long sequenceId = buf.readLong();
        int payloadLength = buf.readInt();

        if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_SIZE) {
            log.warn("Invalid payload length: {} (max={}), closing channel", payloadLength, MAX_PAYLOAD_SIZE);
            ctx.close();
            return;
        }
        if (payloadLength > buf.readableBytes()) {
            log.warn("Payload length {} exceeds readable bytes {}", payloadLength, buf.readableBytes());
            return;
        }

        byte[] payloadData = new byte[payloadLength];
        if (payloadLength > 0) {
            buf.readBytes(payloadData);
        }

        // 转换为Protobuf MessageWrapper (内部统一使用Protobuf)
        MessageId messageId;
        try {
            messageId = MessageId.forNumber(messageIdValue);
        } catch (Exception e) {
            // forNumber may return null for unknown values
            messageId = MessageId.MESSAGE_UNKNOWN;
        }

        if (messageId == null) {
            messageId = MessageId.MESSAGE_UNKNOWN;
        }

        MessageWrapper wrapper = MessageWrapper.newBuilder()
                .setMessageId(messageId)
                .setProtocolVersion(protocolVersion)
                .setSequenceId(sequenceId)
                .setPayload(ByteString.copyFrom(payloadData))
                .build();

        out.add(wrapper);
    }
}
