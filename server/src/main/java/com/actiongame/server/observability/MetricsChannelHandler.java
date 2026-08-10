package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class MetricsChannelHandler extends ChannelDuplexHandler {
    private final ServerMetrics metrics;

    public MetricsChannelHandler(ServerMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof MessageWrapper) {
            metrics.onMessageReceived();
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof MessageWrapper) {
            metrics.onMessageSent();
        }
        ctx.write(msg, promise);
    }
}
