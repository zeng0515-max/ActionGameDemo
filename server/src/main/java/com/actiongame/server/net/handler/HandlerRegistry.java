package com.actiongame.server.net.handler;

import com.actiongame.server.net.session.ConnectionManager;
import com.actiongame.server.net.session.GameSession;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理器注册中心 - 管理消息ID到Handler的映射
 */
public class HandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(HandlerRegistry.class);

    private final ConcurrentHashMap<MessageId, IMessageHandler> handlers = new ConcurrentHashMap<>();
    private final ConnectionManager connectionManager;

    /** 不需要认证即可调用的消息 */
    private static final Set<MessageId> PUBLIC_MESSAGES = Set.of(
        MessageId.LOGIN_REQ,
        MessageId.HEARTBEAT_REQ
    );

    public HandlerRegistry() {
        this.connectionManager = null;
    }

    public HandlerRegistry(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * 注册处理器
     */
    public void register(IMessageHandler handler) {
        MessageId id = handler.getMessageId();
        if (handlers.containsKey(id)) {
            log.warn("Handler already registered for {}, overwriting", id);
        }
        handlers.put(id, handler);
        log.info("Registered handler for: {}", id);
    }

    /**
     * 获取处理器
     */
    public IMessageHandler getHandler(MessageId messageId) {
        return handlers.get(messageId);
    }

    /**
     * 分发消息到对应处理器
     */
    public boolean dispatch(ChannelHandlerContext ctx, MessageWrapper wrapper) {
        MessageId msgId = wrapper.getMessageId();

        if (msgId == MessageId.MESSAGE_UNKNOWN || msgId == null) {
            log.warn("Unknown messageId from channel: {}", ctx.channel().id());
            return false;
        }

        IMessageHandler handler = handlers.get(msgId);
        if (handler == null) {
            log.warn("No handler registered for messageId: {}", msgId);
            return false;
        }

        // 认证检查 (公开消息跳过)
        if (!PUBLIC_MESSAGES.contains(msgId) && connectionManager != null) {
            GameSession session = connectionManager.getSession(ctx.channel());
            if (session == null || !session.isAuthenticated()) {
                log.warn("Unauthenticated message {} from channel {}, closing",
                    msgId, ctx.channel().id());
                ctx.close();
                return false;
            }
        }

        try {
            handler.handle(ctx, wrapper, wrapper.getPayload().toByteArray());
        } catch (Exception e) {
            log.error("Error handling message: {}", msgId, e);
            return false;
        }

        return true;
    }
}
