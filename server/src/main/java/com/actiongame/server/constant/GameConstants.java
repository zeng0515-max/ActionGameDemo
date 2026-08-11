package com.actiongame.server.constant;

/**
 * 游戏全局常量
 */
public final class GameConstants {

    private GameConstants() {}

    /** 协议版本 (语义化: Major=1, Minor=0 → 10) */
    public static final int PROTOCOL_VERSION = 10;

    /** 服务端支持的最低协议版本 */
    public static final int SERVER_MIN_VERSION = 10;

    /** 服务端支持的最高协议版本 */
    public static final int SERVER_MAX_VERSION = 20;

    /** 帧循环固定步长 (ms), 20ms = 50fps */
    public static final int FRAME_INTERVAL_MS = 20;

    /** 心跳超时时间 (ms) */
    public static final int HEARTBEAT_TIMEOUT_MS = 10000;

    /** 心跳检查间隔 (ms) */
    public static final int HEARTBEAT_CHECK_INTERVAL_MS = 3000;

    /** 连续异常消息阈值 (触发断开连接) */
    public static final int MAX_ABNORMAL_MESSAGES = 10;

    /** WebSocket路径 */
    public static final String WEBSOCKET_PATH = "/game";

    /** 默认房间ID */
    public static final String DEFAULT_ROOM_ID = "room-001";

    /** Boss校招Demo标识 */
    public static final String SERVER_NAME = "ActionGameDemo-Server";
    public static final String SERVER_VERSION = "1.0.0-SNAPSHOT";
}
