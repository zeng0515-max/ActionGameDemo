# ActionGameDemo Server

Java权威战斗服务端 — 状态同步架构 (WebSocket + Protobuf)

## 环境要求

- JDK 17+
- Maven 3.8+

## 构建

```bash
cd server
mvn clean package
```

## 运行

```bash
java -jar target/action-game-server-1.0.0-SNAPSHOT.jar [port]
```

默认端口: 9090

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SERVER_PORT` | `9090` | WebSocket 服务端口 |
| `METRICS_PORT` | `9091` | health/metrics/alerts 端口 |
| `NODE_ID` | `local` | 节点标识，用于日志与后续房间路由 |
| `NODE_PUBLIC_ADDRESS` | `localhost:9090` | 节点对外地址提示，用于跨节点重定向 |
| `JAVA_OPTS` | JVM 参数 | 堆与 GC 配置 |

## WebSocket 连接

- URL: `ws://127.0.0.1:9090/game`
- 协议: Binary WebSocket frames
- 消息格式: MessageWrapper (Protobuf)

## 消息流程

```
Client                              Server
  |                                    |
  |--- WebSocket Connect --------------->|
  |                                    |
  |--- LoginReq (protocol_version) ---->|
  |<-- LoginResp (version range) -------|
  |                                    |
  |--- HeartbeatReq ------------------->|
  |<-- HeartbeatResp (rtt) -------------|
  |                                    |
  |--- JoinRoomReq -------------------->|
  |<-- JoinRoomResp -------------------|
  |                                    |
  |--- PlayerActionReq ---------------->|
  |<-- PlayerActionResp ----------------|
  |<-- BattleFrameNotify ---------------|
  |                                    |
```

## 项目结构

```
server/
├── pom.xml
├── src/main/proto/           # Protobuf 协议定义
│   ├── common.proto          # 基础数据结构
│   ├── message_wrapper.proto # 消息信封 + 消息ID枚举
│   ├── login.proto           # 登录协议
│   ├── room.proto            # 房间协议
│   ├── battle.proto          # 战斗协议
│   └── system.proto          # 心跳/系统协议
├── src/main/java/com/actiongame/server/
│   ├── Bootstrap.java        # 启动入口
│   ├── constant/             # 常量
│   └── net/                   # 网络层
│       ├── WebSocketServer.java
│       ├── WebSocketServerHandler.java
│       ├── codec/             # 消息编解码
│       ├── handler/           # 消息处理器
│       ├── session/           # 连接管理
│       └── util/              # 工具类
└── src/test/java/             # 单元测试
```

## 架构红线

所有战斗判定逻辑（伤害计算、Buff效果、命中检测、AI决策、死亡判定）仅存在于Java服务端。
Unity客户端仅负责输入采集、视觉表现、动画驱动与UI展示。
