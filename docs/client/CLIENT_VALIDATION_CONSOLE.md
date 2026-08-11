# Unity 验证客户端设计说明

## 定位

Unity 客户端不是动作游戏本体，而是 **Realtime Combat Server 验证台**。

它的职责是：

- 验证 Java 权威战斗服务端的完整协议链路
- 展示连接、登录、入房、帧同步、动作回包、RTT、带宽等运行指标
- 一键执行端到端自动验证并生成报告
- 不承担任何战斗判定、建模或美术打磨

## 与校招 JD 的对应关系

| JD 能力点 | 验证台对应验证内容 |
|---|---|
| Java + Netty + WebSocket | 连接状态、消息收发计数、断线事件 |
| 二进制协议/Protobuf | 协议日志、序列化错误统计 |
| 帧同步 vs 状态同步 | 帧速率、帧序号、帧快照字节数 |
| 断线重连与延迟优化 | RTT 采样、重连次数、自动重连状态 |
| 分布式与 K8s 加分项 | JoinRoom 重定向次数、目标节点展示 |
| 可观测性与告警 | /health、/metrics、/alerts 探针 |
| 动手能力与工程习惯 | Auto Validate 一键验证 + 报告文件 |

## 界面与数据

验证台在运行时自动创建，不依赖场景、美术资源或 UI 预制体。

面板展示：

- State：Disconnected / Connecting / Connected / Authenticated
- Server、PlayerId、RoomId、EntityId
- RTT：当前值、均值、最小值、最大值
- Frame：当前帧速率、累计帧数、最后帧序号
- Message：收/发消息数与字节数
- Action：发送、接受、拒绝数量
- Error / Redirect / Reconnect 计数
- Protocol Log：最近 300 条协议事件

## 手动流程

1. 启动服务端，默认 `ws://127.0.0.1:9090/game`
2. 在 Unity 打开 `Assets/Scenes/Test_Stage0_Movement.unity`
3. 点击 Play，验证台自动出现并自动连接
4. 使用按钮执行 Connect、Login + Join、Probe HTTP
5. 使用键盘操作发送战斗输入：WASD、左键、K、L、Shift、Space

## 自动验证流程

点击 `Auto Validate` 后依次执行：

1. Connect：建立 WebSocket 连接
2. Login：等待认证成功
3. JoinRoom：匹配并加入房间，处理节点重定向
4. BattleStart：等待战斗开始通知或帧流到达
5. ActionPipeline：连续发送 8 次动作，校验 PlayerActionResp 被接受
6. FrameStream：统计 3 秒内帧接收速率，要求不低于 30fps
7. Metrics：探测 /health、/metrics、/alerts
8. Report：输出 PASS/FAIL 报告并写入
   `Application.persistentDataPath/validation-report-<时间戳>.txt`

## 检查项

- connect+login：认证成功
- join-room：JoinRoomResp code=0
- battle-start：收到 BattleStartNotify 或帧快照
- action-pipeline：PlayerActionResp accepted
- frame-stream：帧速率不低于 30fps
- health/metrics/alerts：HTTP 200 且包含关键字段

## 代码位置

```text
Assets/Scripts/Client/
├── Network/
│   ├── WebSocketClient.cs        # 原生 WebSocket 连接层
│   ├── NetworkClient.cs          # 登录/心跳/入房/战斗消息
│   ├── NetworkTestComponent.cs   # 自动连接与桥初始化
│   └── FrameSnapshotManager.cs   # 帧快照缓存
└── Diagnostics/
    ├── NetworkDiagnostics.cs     # 协议指标与日志
    ├── ServerValidationPanel.cs  # IMGUI 验证面板
    └── AutoValidationRunner.cs   # 端到端自动验证
```

## 不做什么

- 不实现客户端战斗判定
- 不依赖美术模型、动画资源、VFX 资源
- 不改动服务端权威逻辑
