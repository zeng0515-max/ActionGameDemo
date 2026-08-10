package com.actiongame.server.net;

import com.actiongame.server.net.handler.HandlerRegistry;
import com.actiongame.server.net.session.ConnectionManager;
import com.actiongame.server.net.session.GameSession;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket业务处理器 - 接收解码后的MessageWrapper并分发到对应Handler
 *
 * Pipeline: WebSocket帧 → MessageDecoder(解析Protobuf) → 本Handler(业务分发)
 * 输出: MessageWrapper → MessageEncoder(序列化为BinaryWebSocketFrame)
 */
public class WebSocketServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServerHandler.class);

    private final ConnectionManager connectionManager;
    private final HandlerRegistry handlerRegistry;
    private final com.actiongame.server.audit.ServerMetrics metrics;

    public WebSocketServerHandler(ConnectionManager connectionManager, HandlerRegistry handlerRegistry) {
        this(connectionManager, handlerRegistry, null);
    }

    public WebSocketServerHandler(ConnectionManager connectionManager, HandlerRegistry handlerRegistry,
                                  com.actiongame.server.audit.ServerMetrics metrics) {
        this.connectionManager = connectionManager;
        this.handlerRegistry = handlerRegistry;
        this.metrics = metrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        GameSession session = connectionManager.createSession(ctx.channel());
        if (metrics != null) metrics.onConnect();
        log.info("Client connected: channelId={}, playerId={}", ctx.channel().id(), session.getPlayerId());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionManager.removeSession(ctx.channel());
        if (metrics != null) metrics.onDisconnect();
        log.info("Client disconnected: channelId={}", ctx.channel().id());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof MessageWrapper wrapper) {
                // 心跳消息更新会话心跳时间
                if (wrapper.getMessageId() == MessageId.HEARTBEAT_REQ) {
                    GameSession session = connectionManager.getSession(ctx.channel());
                    if (session != null) {
                        session.updateHeartbeatTime();
                    }
                }

                // 分发到对应Handler
                handlerRegistry.dispatch(ctx, wrapper);
            } else if (msg instanceof WebSocketFrame) {
                // 非Binary帧 (Text/Ping/Pong等), 忽略
                log.debug("Received non-binary WebSocket frame: {}", msg.getClass().getSimpleName());
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof io.netty.handler.timeout.IdleStateEvent) {
            log.warn("Idle timeout, closing channel: {}", ctx.channel().id());
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in channel {}: {}", ctx.channel().id(), cause.getMessage(), cause);
        if (metrics != null) metrics.onError("WebSocketServerHandler");
        ctx.close();
    }
}
