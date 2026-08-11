package com.actiongame.server.net.codec;

import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.List;

/**
 * 消息编码器 - 将MessageWrapper编码为客户端二进制格式的BinaryWebSocketFrame
 *
 * 输出格式: [消息ID(4,BE)] [协议版本(4,BE)] [序列号(8,BE)] [Payload长度(4,BE)] [Payload]
 */
public class MessageEncoder extends MessageToMessageEncoder<MessageWrapper> {

    private static final Logger log = LoggerFactory.getLogger(MessageEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageWrapper msg, List<Object> out) throws Exception {
        byte[] payloadData = msg.getPayload().toByteArray();
        int payloadLength = payloadData != null ? payloadData.length : 0;

        // 4(id) + 4(version) + 8(seq) + 4(payloadLen) + payload
        ByteBuf buf = Unpooled.buffer(20 + payloadLength);
        buf = buf.order(ByteOrder.BIG_ENDIAN);

        buf.writeInt(msg.getMessageId().getNumber());
        buf.writeInt(msg.getProtocolVersion());
        buf.writeLong(msg.getSequenceId());
        buf.writeInt(payloadLength);

        if (payloadLength > 0) {
            buf.writeBytes(payloadData);
        }

        out.add(new BinaryWebSocketFrame(buf));
    }
}
