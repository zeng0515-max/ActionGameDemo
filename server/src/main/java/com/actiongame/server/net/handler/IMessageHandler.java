package com.actiongame.server.net.handler;

import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.channel.ChannelHandlerContext;

/**
 * 消息处理器接口 - 每种消息类型对应一个Handler
 */
public interface IMessageHandler {

    /**
     * 获取该处理器处理的消息ID
     */
    com.actiongame.server.proto.MessageWrapperProto.MessageId getMessageId();

    /**
     * 处理消息
     *
     * @param ctx      Channel上下文
     * @param wrapper  消息包装器
     * @param payload  内层消息bytes (由各Handler自行反序列化)
     */
    void handle(ChannelHandlerContext ctx, MessageWrapper wrapper, byte[] payload) throws Exception;
}
