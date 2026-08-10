package com.actiongame.server.net;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.net.codec.MessageDecoder;
import com.actiongame.server.net.codec.MessageEncoder;
import com.actiongame.server.net.handler.HandlerRegistry;
import com.actiongame.server.net.session.ConnectionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket服务器 (基于Netty)
 *
 * Pipeline结构:
 *   LoggingHandler (DEBUG)
 *   HttpServerCodec → HttpObjectAggregator → WebSocketServerProtocolHandler (握手)
 *   IdleStateHandler (心跳检测)
 *   MessageDecoder → MessageEncoder (Protobuf编解码)
 *   WebSocketServerHandler (业务分发)
 */
public class WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    private final int port;
    private final ConnectionManager connectionManager;
    private final HandlerRegistry handlerRegistry;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public WebSocketServer(int port, ConnectionManager connectionManager, HandlerRegistry handlerRegistry) {
        this.port = port;
        this.connectionManager = connectionManager;
        this.handlerRegistry = handlerRegistry;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        // HTTP编解码 (WebSocket握手用)
                        pipeline.addLast("httpCodec", new HttpServerCodec());
                        pipeline.addLast("httpAggregator", new HttpObjectAggregator(65536));

                        // WebSocket协议处理 (握手 + 帧)
                        pipeline.addLast("wsProtocol", new WebSocketServerProtocolHandler(
                                GameConstants.WEBSOCKET_PATH, null, true, 65536));

                        // 空闲检测 (读超时 = 心跳超时)
                        pipeline.addLast("idleState", new IdleStateHandler(
                                GameConstants.HEARTBEAT_TIMEOUT_MS, 0, 0, TimeUnit.MILLISECONDS));

                        // Protobuf编解码
                        pipeline.addLast("decoder", new MessageDecoder());
                        pipeline.addLast("encoder", new MessageEncoder());

                        // 业务处理器
                        pipeline.addLast("serverHandler", new WebSocketServerHandler(connectionManager, handlerRegistry));
                    }
                });

        ChannelFuture future = bootstrap.bind(port).sync();
        serverChannel = future.channel();
        log.info("WebSocket server listening on ws://0.0.0.0:{}{}", getPort(), GameConstants.WEBSOCKET_PATH);
    }

    public int getPort() {
        if (serverChannel == null) {
            throw new IllegalStateException("Server not started");
        }
        return ((java.net.InetSocketAddress) serverChannel.localAddress()).getPort();
    }

    public void shutdown() {
        log.info("Shutting down WebSocket server...");

        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }

        try {
            if (bossGroup != null) {
                bossGroup.terminationFuture().sync();
            }
            if (workerGroup != null) {
                workerGroup.terminationFuture().sync();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("WebSocket server shut down");
    }
}
