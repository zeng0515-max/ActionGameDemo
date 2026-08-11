package com.actiongame.server.net.handler;

import com.actiongame.server.auth.AuthService;
import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.net.session.ConnectionManager;
import com.actiongame.server.net.session.GameSession;
import com.actiongame.server.net.util.BinaryCodec;
import com.actiongame.server.net.util.MessageHelper;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 登录处理器 - 处理登录请求 + 协议版本协商
 *
 * LoginReq 二进制格式: [protocolVersion(4)] [token(str)] [playerName(str)] [characterConfigId(4)]
 * LoginResp 二进制格式: [protocolVersion(4)] [code(4)] [message(str)] [playerId(str)] [sessionKey(str)] [serverMinVersion(4)] [serverMaxVersion(4)]
 */
public class LoginHandler implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);

    private final ConnectionManager connectionManager;
    private final com.actiongame.server.audit.AuditLoggerImpl auditLogger;
    private final AuthService authService;

    public LoginHandler(ConnectionManager connectionManager) {
        this(connectionManager, null, new AuthService());
    }

    public LoginHandler(ConnectionManager connectionManager, com.actiongame.server.audit.AuditLoggerImpl auditLogger) {
        this(connectionManager, auditLogger, new AuthService());
    }

    public LoginHandler(ConnectionManager connectionManager,
                        com.actiongame.server.audit.AuditLoggerImpl auditLogger,
                        AuthService authService) {
        this.connectionManager = connectionManager;
        this.auditLogger = auditLogger;
        this.authService = authService;
    }

    @Override
    public MessageId getMessageId() {
        return MessageId.LOGIN_REQ;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, MessageWrapper wrapper, byte[] payload) throws Exception {
        GameSession session = connectionManager.getSession(ctx.channel());

        if (session == null) {
            log.error("No session found for channel: {}", ctx.channel().id());
            ctx.close();
            return;
        }

        // 解析LoginReq (客户端二进制格式)
        int offset = 0;
        int protocolVersion = BinaryCodec.readInt(payload, offset); offset += 4;
        String token = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(token);
        String playerName = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(playerName);
        int characterConfigId = BinaryCodec.readInt(payload, offset);

        log.info("Login request: playerName={}, protocolVersion={}, token={}", playerName, protocolVersion, token);

        // 协议版本协商
        boolean versionOk = protocolVersion >= GameConstants.SERVER_MIN_VERSION
                && protocolVersion <= GameConstants.SERVER_MAX_VERSION;

        // 构建LoginResp (客户端二进制格式)
        String respMessage;
        String playerId = "";
        String sessionKey = "";
        int code;

        if (!authService.validate(token)) {
            code = -2;
            respMessage = "Invalid token";

            log.warn("Login rejected: invalid token for playerName={}", playerName);
            if (auditLogger != null) auditLogger.logLogin("", false, "invalid token");
        } else if (versionOk) {
            session.setAuthenticated(true);
            session.setNegotiatedVersion(protocolVersion);
            session.updateHeartbeatTime();

            playerId = session.getPlayerId();
            sessionKey = UUID.randomUUID().toString();
            code = 0;
            respMessage = "Login success";

            log.info("Login success: playerId={}, sessionKey={}, negotiatedVersion={}",
                    playerId, sessionKey, protocolVersion);
            if (auditLogger != null) auditLogger.logLogin(playerId, true, "version=" + protocolVersion);
        } else {
            code = -1;
            respMessage = "Protocol version mismatch. Server supports "
                    + GameConstants.SERVER_MIN_VERSION + "-" + GameConstants.SERVER_MAX_VERSION
                    + ", client sent " + protocolVersion;

            log.warn("Login rejected: version mismatch (client={}, server={}-{})",
                    protocolVersion, GameConstants.SERVER_MIN_VERSION, GameConstants.SERVER_MAX_VERSION);
            if (auditLogger != null) auditLogger.logLogin("", false, "version mismatch: " + protocolVersion);
        }

        // 计算响应大小
        int respSize = 4 + 4 + BinaryCodec.stringSize(respMessage)
                + BinaryCodec.stringSize(playerId) + BinaryCodec.stringSize(sessionKey) + 4 + 4;
        byte[] respPayload = new byte[respSize];
        offset = 0;
        BinaryCodec.writeInt(respPayload, offset, GameConstants.PROTOCOL_VERSION); offset += 4;
        BinaryCodec.writeInt(respPayload, offset, code); offset += 4;
        BinaryCodec.writeString(respPayload, offset, respMessage); offset += BinaryCodec.stringSize(respMessage);
        BinaryCodec.writeString(respPayload, offset, playerId); offset += BinaryCodec.stringSize(playerId);
        BinaryCodec.writeString(respPayload, offset, sessionKey); offset += BinaryCodec.stringSize(sessionKey);
        BinaryCodec.writeInt(respPayload, offset, GameConstants.SERVER_MIN_VERSION); offset += 4;
        BinaryCodec.writeInt(respPayload, offset, GameConstants.SERVER_MAX_VERSION);

        MessageWrapper respWrapper = MessageHelper.wrap(
                MessageId.LOGIN_RESP,
                session.nextSequenceId(),
                respPayload
        );

        ctx.writeAndFlush(respWrapper);
    }
}
