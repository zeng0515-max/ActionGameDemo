package com.actiongame.server.net.session;

import io.netty.channel.Channel;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 游戏会话 - 封装单个WebSocket连接的状态
 */
public class GameSession {

    private final String playerId;
    private final Channel channel;
    private volatile boolean authenticated = false;

    /** 消息序列号 (防重放/防丢序) */
    private final AtomicLong sequenceId = new AtomicLong(0);

    /** 最近一次心跳时间 */
    private volatile long lastHeartbeatTime;

    /** 协商后的协议版本 */
    private volatile int negotiatedVersion = 0;

    /** 玩家当前所在房间 */
    private volatile String roomId;

    public GameSession(String playerId, Channel channel) {
        this.playerId = playerId;
        this.channel = channel;
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public String getPlayerId() {
        return playerId;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public long nextSequenceId() {
        return sequenceId.incrementAndGet();
    }

    public long getLastSequenceId() {
        return sequenceId.get();
    }

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void updateHeartbeatTime() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public int getNegotiatedVersion() {
        return negotiatedVersion;
    }

    public void setNegotiatedVersion(int version) {
        this.negotiatedVersion = version;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    /**
     * 发送 MessageWrapper 到客户端 (通过 pipeline 编码)
     */
    public void send(com.actiongame.server.proto.MessageWrapperProto.MessageWrapper wrapper) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(wrapper);
        }
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }
}
