package com.actiongame.server.net.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 连接管理器 - 管理所有活跃的WebSocket连接
 */
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    /** playerId → Session 映射 */
    private final ConcurrentHashMap<String, GameSession> sessionsByPlayerId = new ConcurrentHashMap<>();

    /** ChannelId → Session 映射 */
    private final ConcurrentHashMap<ChannelId, GameSession> sessionsByChannelId = new ConcurrentHashMap<>();

    /** 玩家ID生成器 */
    private final AtomicLong playerIdGenerator = new AtomicLong(0);

    /**
     * 创建新会话
     */
    public GameSession createSession(Channel channel) {
        String playerId = "player-" + playerIdGenerator.incrementAndGet();
        GameSession session = new GameSession(playerId, channel);

        sessionsByPlayerId.put(playerId, session);
        sessionsByChannelId.put(channel.id(), session);

        log.info("Session created: playerId={}, channelId={}", playerId, channel.id());
        return session;
    }

    /**
     * 通过Channel获取会话
     */
    public GameSession getSession(Channel channel) {
        return sessionsByChannelId.get(channel.id());
    }

    /**
     * 通过playerId获取会话
     */
    public GameSession getSession(String playerId) {
        return sessionsByPlayerId.get(playerId);
    }

    /**
     * 关闭并移除会话
     */
    public void removeSession(Channel channel) {
        GameSession session = sessionsByChannelId.remove(channel.id());
        if (session != null) {
            sessionsByPlayerId.remove(session.getPlayerId());
            // 关闭底层 Channel, 避免资源泄漏 (CLOSE_WAIT 残留)
            if (channel.isOpen()) {
                channel.close();
            }
            log.info("Session removed: playerId={}", session.getPlayerId());
        }
    }

    /**
     * 获取当前活跃连接数
     */
    public int getActiveConnectionCount() {
        return sessionsByChannelId.size();
    }
}
