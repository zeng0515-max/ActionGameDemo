package com.actiongame.server.benchmark;

import com.actiongame.server.net.codec.MessageDecoder;
import com.actiongame.server.net.codec.MessageEncoder;
import com.actiongame.server.net.util.BinaryCodec;
import com.actiongame.server.net.util.MessageHelper;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestClient {
    private final LoadTestConfig config;
    private final String playerName;
    private final String roomId;
    private final LoadTestClientStats stats;
    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<Long, Long> pendingActionTimestamps = new ConcurrentHashMap<>();

    private volatile int entityId = -1;
    private volatile Channel channel;

    public LoadTestClient(LoadTestConfig config, int clientIndex) {
        this.config = config;
        this.playerName = "bench-" + clientIndex;
        this.roomId = config.roomIdForClient(clientIndex);
        this.stats = new LoadTestClientStats(playerName);
    }

    public LoadTestClientStats run(NioEventLoopGroup group) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    URI uri = URI.create("ws://" + config.getHost() + ":" + config.getPort() + "/game");
                    WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
                    ch.pipeline().addLast(new HttpClientCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(65536));
                    ch.pipeline().addLast(new WebSocketClientProtocolHandler(handshaker));
                    ch.pipeline().addLast(new ByteCountingHandler());
                    ch.pipeline().addLast(new MessageDecoder());
                    ch.pipeline().addLast(new MessageEncoder());
                    ch.pipeline().addLast(new ClientHandler());
                }
            });

        channel = bootstrap.connect(config.getHost(), config.getPort()).sync().channel();
        boolean ready = readyLatch.await(10, TimeUnit.SECONDS);
        if (!ready || channel == null || !channel.isActive()) {
            stats.recordError();
            closeQuietly();
            return stats;
        }

        long endTime = System.currentTimeMillis() + config.getDurationMs();
        long frameIndex = 0;
        while (System.currentTimeMillis() < endTime && channel.isActive()) {
            long seq = sequence.incrementAndGet();
            long sentAt = System.currentTimeMillis();
            pendingActionTimestamps.put(seq, sentAt);
            channel.writeAndFlush(buildActionMessage(seq, frameIndex++));
            stats.recordActionSent();
            Thread.sleep(config.getActionIntervalMs());
        }

        closeQuietly();
        return stats;
    }

    private MessageWrapper buildActionMessage(long seq, long frameIndex) {
        byte[] payload = LoadTestPayloads.buildActionPayload(entityId, frameIndex, 1);
        return MessageHelper.wrap(MessageId.PLAYER_ACTION_REQ, seq, payload);
    }

    private void closeQuietly() {
        if (channel != null) {
            channel.close();
        }
    }

    private class ClientHandler extends SimpleChannelInboundHandler<MessageWrapper> {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                byte[] login = LoadTestPayloads.buildLoginPayload("bench-token", playerName);
                ctx.writeAndFlush(MessageHelper.wrap(MessageId.LOGIN_REQ, sequence.incrementAndGet(), login));
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MessageWrapper msg) {
            switch (msg.getMessageId()) {
                case LOGIN_RESP -> {
                    byte[] payload = msg.getPayload().toByteArray();
                    int code = BinaryCodec.readInt(payload, 4);
                    if (code != 0) {
                        stats.recordError();
                        readyLatch.countDown();
                        return;
                    }
                    byte[] join = LoadTestPayloads.buildJoinRoomPayload(playerName, roomId);
                    ctx.writeAndFlush(MessageHelper.wrap(MessageId.JOIN_ROOM_REQ, sequence.incrementAndGet(), join));
                }
                case JOIN_ROOM_RESP -> {
                    LoadTestPayloads.JoinResult result = LoadTestPayloads.parseJoinResponse(msg.getPayload().toByteArray());
                    if (result.code() != 0) {
                        stats.recordError();
                        readyLatch.countDown();
                        return;
                    }
                    entityId = result.entityId();
                    readyLatch.countDown();
                }
                case BATTLE_FRAME_NOTIFY -> stats.recordFrame();
                case PLAYER_ACTION_RESP -> {
                    Long sentAt = pendingActionTimestamps.remove(msg.getSequenceId());
                    if (sentAt != null) {
                        stats.recordActionRtt(System.currentTimeMillis() - sentAt);
                    }
                }
                default -> {}
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            readyLatch.countDown();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            stats.recordError();
            ctx.close();
        }
    }

    private class ByteCountingHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof BinaryWebSocketFrame frame) {
                stats.recordBytesReceived(frame.content().readableBytes());
            }
            ctx.fireChannelRead(msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof BinaryWebSocketFrame frame) {
                stats.recordBytesSent(frame.content().readableBytes());
            }
            ctx.write(msg, promise);
        }
    }
}
