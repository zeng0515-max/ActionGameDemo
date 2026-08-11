package com.actiongame.server.net.util;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;

/**
 * 消息构建辅助工具
 */
public final class MessageHelper {

    private MessageHelper() {}

    /**
     * 包装消息为MessageWrapper (自动填充服务端协议版本)
     */
    public static MessageWrapper wrap(MessageId messageId, long sequenceId, byte[] payload) {
        return MessageWrapper.newBuilder()
                .setMessageId(messageId)
                .setProtocolVersion(GameConstants.PROTOCOL_VERSION)
                .setSequenceId(sequenceId)
                .setPayload(com.google.protobuf.ByteString.copyFrom(payload))
                .build();
    }

    /**
     * 包装消息 (指定协议版本, 用于版本协商回退)
     */
    public static MessageWrapper wrap(MessageId messageId, long sequenceId, int protocolVersion, byte[] payload) {
        return MessageWrapper.newBuilder()
                .setMessageId(messageId)
                .setProtocolVersion(protocolVersion)
                .setSequenceId(sequenceId)
                .setPayload(com.google.protobuf.ByteString.copyFrom(payload))
                .build();
    }
}
