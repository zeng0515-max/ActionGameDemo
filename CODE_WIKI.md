# ActionGameDemo 项目百科

## 一、项目概述

ActionGameDemo 是 Realtime Combat Server
一个以实时对战服务端为核心产品的后端项目

客户端负责表现与输入
服务端负责权威判定
通信基于 WebSocket 二进制协议

### 核心特性

- 50fps 帧同步状态广播
- WebSocket 二进制协议与 Netty Pipeline
- 服务端权威战斗计算
- 房间管理、帧调度与断线重连
- 实时反作弊三层检测
- AI 敌人 FSM + Boss 阶段
- 战斗回放、审计与 LLM 复盘
- Docker 一键部署

## 二、技术架构

### 2.1 整体架构图

```
┌─────────────────┐      WebSocket       ┌─────────────────┐
│   Unity Client  │ ◄──────────────────► │  Java Server    │
│   (C#)          │   Binary Protocol    │  (Netty)        │
├─────────────────┤                      ├─────────────────┤
│  Input Layer    │                      │  Netty Pipeline │
│  Presentation   │                      │  Message Router │
│  VFX / Camera   │                      │  Battle Room    │
│  UI Panels      │                      │  Combat System  │
└─────────────────┘                      └─────────────────┘
```

### 2.2 架构红线

所有战斗判定逻辑
仅在 Java 服务端执行

客户端禁止修改权威数值
只负责输入采集与视觉表现

### 2.3 后端能力对齐

| JD 能力点 | CODE_WIKI 对应章节 |
|---|---|
| 网络编程/高并发 | 四.2 Net 网络层、五.2 帧同步机制 |
| 服务端架构设计 | 四.3 Room、四.4 Battle |
| 稳定性与安全 | 四.9 AntiCheat、四.12 Audit |
| 数据库/中间件 | persistence：MatchResultRepository、RoomStateCache |
| 性能优化 | 十六 关键性能考量、docs/benchmark/BENCHMARK_2026-08-10.md |
| AI 应用 | 四.10 LLM 服务 |

## 三、客户端模块（Unity C#）

### 3.1 Core 核心层

位于 `Assets/Scripts/Core/`

**GameManager**
- 全局游戏状态机
- 管理 Menu / Playing / Paused / GameOver / Victory
- 控制 timeScale 与输入锁定
- 通过 EventManager 广播状态变更

**EventManager**
- 全局事件总线
- 支持游戏状态变化事件
- 支持战斗开始/结束事件

**SceneBootstrap**
- 场景加载时自动初始化
- 创建 GameManager 与 EventManager
- 为主相机挂载 CameraRig

**Singleton<T>**
- 单例模式泛型基类
- 所有管理器均继承此类

**SaveManager / SaveData**
- 本地存档读写
- JSON 序列化存储

**ObjectPool**
- 通用对象池实现
- 供 VFXEffectManager 使用

### 3.2 Protocol 协议层

位于 `Assets/Scripts/Protocol/`

**IMessage**
- 所有网络消息基接口
- 定义 GetMessageId / ToByteArray / FromByteArray

**MessageId 枚举**
- 登录 1001-1099
- 房间 1101-1199
- 战斗 1201-1299
- 系统 2001-2099

**Messages.cs**
- 定义具体消息结构体
- LoginReq / LoginResp
- JoinRoomReq / JoinRoomResp
- PlayerActionReq / PlayerActionResp
- BattleFrameNotify / BattleStartNotify / BattleEndNotify

**DataStructures.cs**
- 网络数据结构定义
- Vector3Proto / QuaternionProto
- CharacterSnapshot / CharacterStatsSnapshot
- BuffSnapshot / DamageNumberEventData / VFXEventData

**BinarySerializer**
- 自定义二进制序列化
- 支持 int / float / long / string

**MessageWrapper**
- 消息信封封装
- 包含消息ID、序列号、协议版本、Payload

### 3.3 Client 网络层

位于 `Assets/Scripts/Client/Network/`

**NetworkClient**
- WebSocket 连接管理
- 消息分发处理器注册
- 心跳与自动重连机制
- 登录 / 加入房间 / 发送玩家操作

**WebSocketClient**
- 底层 WebSocket 实现
- 异步连接与发送

**FrameSnapshotManager**
- 接收服务端帧快照
- 缓冲最近 3 帧数据
- 提供查询接口供表现层读取
- 消费伤害事件与 VFX 事件

### 3.4 Player 输入层

位于 `Assets/Scripts/Player/`

**PlayerInputHandler**
- 唯一输入入口
- 采集移动、跳跃、攻击、技能、闪避等按键
- 支持输入锁定（暂停/结算时屏蔽）
- 消费模式防止一帧多消费

**NetworkInputBridge**
- 将本地输入转换为网络请求
- 发送 PlayerActionReq 到服务端

### 3.5 Presentation 表现层

位于 `Assets/Scripts/Presentation/`

**CharacterPresentation**
- 绑定 EntityId 与游戏对象
- 从帧快照读取位置与旋转
- 远程玩家使用插值平滑
- 根据状态驱动 Animator

**BattleEventConsumer**
- 全局战斗事件消费者
- 纯表现，不做判定
- 消费伤害飘字与特效事件

### 3.6 Combat 战斗表现

位于 `Assets/Scripts/Combat/`

**VFXEffectManager**
- 全局特效管理单例
- 对象池管理特效预制体
- 支持世界坐标播放与目标跟随
- 支持元素颜色染色
- 自动检测粒子生命周期

**VFXEffectType 枚举**
- 定义所有特效类型
- Slash / HitImpact / CriticalHit / Dodge / Death
- SkillCast / UltimateCast / BuffAppear 等

### 3.7 Camera 相机系统

位于 `Assets/Scripts/Camera/`

**CameraRig**
- 第三人称跟随相机
- 鼠标右键控制旋转
- 平滑阻尼与碰撞检测
- 支持锁定目标时自动朝向

**LockOnSystem**
- 目标锁定逻辑
- Q 键触发锁定切换

### 3.8 UI 界面层

位于 `Assets/Scripts/UI/`

**UIManager**
- 面板栈管理
- 根据游戏状态自动切换面板
- 处理菜单、暂停、设置面板

**UIBase**
- 所有 UI 面板基类
- 提供 Show / Hide 抽象

**MainMenuPanel / PausePanel / SettingsPanel**
- 具体面板实现
- 绑定按钮回调到 UIManager

### 3.9 Editor 编辑器工具

位于 `Assets/Scripts/Editor/`

**StaticModelBuilder**
- 静态模型构建工具

**MixamoAnimationSetup**
- Mixamo 动画自动配置

**NineTailedFoxGenerator**
- 九尾狐角色生成器

**AnimationClipCreator**
- 动画片段创建工具

**ModelPromptConfig**
- 模型提示词配置

## 四、服务端模块（Java）

### 4.1 入口与启动

**Bootstrap**
- 服务器启动入口
- 默认端口 9090
- 初始化 ConnectionManager、AuditLogger、HandlerRegistry
- 注册所有消息处理器
- 添加 JVM 关闭钩子

### 4.2 Net 网络层

位于 `server/src/main/java/com/actiongame/server/net/`

**WebSocketServer**
- 基于 Netty 的 WebSocket 服务器
- Pipeline 结构：
  - LoggingHandler
  - HttpServerCodec
  - HttpObjectAggregator
  - WebSocketServerProtocolHandler
  - IdleStateHandler（心跳超时检测）
  - MessageDecoder / MessageEncoder
  - WebSocketServerHandler

**WebSocketServerHandler**
- 业务消息分发
- 连接生命周期管理

**HandlerRegistry**
- 消息处理器注册中心
- 根据 MessageId 路由到对应 Handler

**IMessageHandler<T>**
- 处理器接口
- 具体实现：LoginHandler / HeartbeatHandler / JoinRoomHandler / PlayerActionHandler

**ConnectionManager / GameSession**
- WebSocket 连接管理
- 会话状态维护
- 序列号生成

**MessageDecoder / MessageEncoder**
- Protobuf 编解码

### 4.3 Room 房间系统

位于 `server/src/main/java/com/actiongame/server/room/`

**BattleRoom**
- 核心战斗调度单元
- 管理玩家列表、怪物列表
- 实现 FrameExecutor 接口
- 每帧执行：处理操作 → AI更新 → Buff更新 → 资源回收 → 生成快照 → 广播帧

**RoomManager**
- 房间创建与销毁
- 玩家匹配与分配

**FrameScheduler**
- 固定帧率调度器（20ms = 50fps）
- 驱动 BattleRoom.executeFrame

**FrameExecutor**
- 帧执行接口
- executeFrame(long frameIndex, float deltaTime)

**BattleFrame**
- 单帧快照数据
- 包含所有角色状态、伤害事件、特效事件

**BattleFrameSerializer**
- 帧快照二进制序列化
- 延迟序列化优化（只序列化一次，多玩家共享）

**RoomPlayer / RoomMonster**
- 房间内玩家/怪物封装
- RoomMonster 包含 AI 控制器引用

**ResourceRecycler**
- 死亡怪物回收
- 离线玩家清理

**RoomStatus 枚举**
- WAITING / BATTLE / SETTLEMENT / CLOSED

### 4.4 Battle 战斗系统

位于 `server/src/main/java/com/actiongame/server/battle/`

**CombatSystem**
- 战斗系统主类
- 协调伤害管线、命中检测、Buff引擎
- executeAttack：单体攻击
- executeAreaAttack：范围攻击

**DamagePipeline**
- 伤害计算管线
- 步骤：BaseDamageStep → ElementDamageStep → CriticalDamageStep → DefenseDamageStep

**DamageContext**
- 伤害计算上下文
- 包含攻击方与防御方属性

**HitDetectionSystem**
- 命中检测
- 基于距离的范围检测

**ComboSystem / ComboScorer / ComboGrade**
- 连击系统
- 连击评分与评级

### 4.5 Buff 引擎

位于 `server/src/main/java/com/actiongame/server/battle/buffengine/`

**BuffEngine**
- 角色 Buff 管理
- addBuff / update / remove
- 支持叠加规则

**IBuffEffectHandler**
- Buff 效果处理器接口

**具体效果实现**
- AttributeBuffEffect：属性加成
- DotBuffEffect：持续伤害
- ControlBuffEffect：控制效果
- ShieldBuffEffect：护盾效果

**BuffFactory**
- Buff 实例工厂

### 4.6 AI 系统

位于 `server/src/main/java/com/actiongame/server/ai/`

**EnemyAIController**
- 普通敌人 AI 决策

**BossAIController**
- Boss AI 决策（继承 EnemyAIController）

**TargetSelector**
- 目标选择策略

**FSM 状态机**
- EnemyState：状态基类
- IdleState：待机
- PatrolState：巡逻
- ChaseState：追击
- FleeState：逃跑
- HurtState：受击
- DeadState：死亡
- EnemyStateType：状态枚举

**SimplePathfinder**
- 简单寻路实现

### 4.7 Domain 领域模型

位于 `server/src/main/java/com/actiongame/server/domain/`

**Character（抽象基类）**
- entityId / position / rotation / state / stats
- takeDamage / isDead / getElementType

**PlayerCharacter（继承 Character）**
- playerId / sessionKey

**MonsterCharacter（继承 Character）**
- monsterConfigId

**CharacterStats**
- maxHealth / attackPower / defense / moveSpeed
- criticalRate / criticalDamageMultiplier
- effectiveAttackPower / effectiveDefense 等计算属性

**CharacterState 枚举**
- IDLE / MOVE / JUMP / ATTACK / SKILL / ULTIMATE / DODGE / HURT / DEAD

**Skill / SkillCost / SkillType**
- 技能定义与消耗

**Buff / BuffType / BuffStackingRule**
- Buff 定义与叠加规则

**DamageInfo / HitResult / DamageType**
- 伤害信息与命中结果

**ElementType 枚举**
- NONE / FIRE / ICE / LIGHTNING / WATER

### 4.8 Config 配置系统

位于 `server/src/main/java/com/actiongame/server/config/`

**ConfigLoader**
- JSON 配置加载
- 基于 Gson 解析

**配置数据类**
- CharacterConfig / MonsterConfig
- SkillConfig / BuffConfig
- EquipmentConfig / ElementConfig

### 4.9 AntiCheat 反作弊

位于 `server/src/main/java/com/actiongame/server/anticheat/`

**CheatDetector**
- 三层检测架构：
  - Layer 1：实时检测（位置/速度异常）
  - Layer 2：逻辑校验（伤害异常）
  - Layer 3：回放分析

**PlayerBehaviorTracker**
- 玩家行为追踪

**CheatResponseManager**
- 作弊事件响应

**CheatIncident / CheatType / CheatSeverity**
- 作弊事件定义与分级

**AntiCheatConfig**
- 反作弊阈值配置

### 4.10 LLM 服务

位于 `server/src/main/java/com/actiongame/server/llm/`

**LLMService（接口）**
- generateBattleSummary：战斗总结
- analyzeDifficulty：难度分析
- analyzeCheatPatterns：作弊模式分析

**LocalLLMService**
- 本地 LLM 实现

**BattleContext**
- LLM 分析上下文
- 包含房间ID、帧数、时长、玩家数、怪物数、作弊事件

**DifficultySuggestion / CheatAnalysis**
- 分析结果数据结构

### 4.11 Replay 回放系统

位于 `server/src/main/java/com/actiongame/server/replay/`

**BattleRecorder**
- 战斗录制

**BattleRecording**
- 录制数据存储

**ReplayStorageManager**
- 回放文件管理

**BattleReplayExecutor**
- 回放执行器

### 4.12 Audit 审计日志

位于 `server/src/main/java/com/actiongame/server/audit/`

**IAuditLogger**
- 审计日志接口

**AuditLoggerImpl**
- 审计日志实现

**AuditLogEntry / AuditLogType**
- 日志条目与类型定义

**ServerMetrics**
- 服务端性能指标

### 4.13 Util 工具类

位于 `server/src/main/java/com/actiongame/server/util/`

**Vector3 / Quaternion**
- 服务端数学结构（独立于 Unity）

**MathUtils / RandomUtil**
- 数学与随机工具

## 五、网络协议

### 5.1 连接流程

```
Client              Server
  |                   |
  |--- WebSocket 连接-->|
  |                   |
  |--- LoginReq ----->|
  |<-- LoginResp -----|
  |                   |
  |--- HeartbeatReq ->|
  |<-- HeartbeatResp -|
  |                   |
  |--- JoinRoomReq -->|
  |<-- JoinRoomResp --|
  |                   |
  |--- PlayerAction ->|
  |<-- BattleFrame ---|
```

### 5.2 帧同步机制

服务端以 20ms 间隔
固定帧率广播 BattleFrameNotify

客户端接收帧快照
更新角色位置、状态、动画

### 5.3 消息格式

所有消息采用 MessageWrapper 封装
包含以下字段：
- MessageId（消息类型）
- SequenceId（序列号）
- ProtocolVersion（协议版本）
- Payload（二进制负载）

## 六、项目依赖

### 6.1 Unity 客户端

核心包：
- com.unity.ai.navigation 1.1.7
- com.unity.textmeshpro 3.0.9
- com.unity.ugui 1.0.0

扩展包：
- cn.tuanjie.ai.generators（本地文件）
- cn.tuanjie.codely.bridge 1.0.73

### 6.2 Java 服务端

核心框架：
- Netty 4.1.108.Final（WebSocket 网络层）
- Protobuf 3.25.3（序列化）

日志：
- SLF4J 2.0.13
- Logback 1.5.6

配置：
- Gson 2.11.0（JSON 解析）

测试：
- JUnit 5.10.2
- AssertJ 3.26.0
- Mockito 5.12.0

构建：
- Maven Shade Plugin（可执行 JAR）

## 七、项目运行方式

### 7.1 服务端启动

**方式一：直接运行**

```bash
cd server
mvn clean package
java -jar target/action-game-server-1.0.0-SNAPSHOT.jar [port]
```

默认端口：9090

**方式二：Docker 部署**

```bash
docker-compose up -d
```

环境变量：
- SERVER_PORT=9090
- JAVA_OPTS=-Xms256m -Xmx512m

### 7.2 客户端启动

使用 Unity Editor 打开项目
加载 Test_Stage0_Movement 场景
点击 Play 运行

确保服务端已启动
客户端默认连接 127.0.0.1:9090

### 7.3 目录结构

```
ActionGameDemo/
├── Assets/
│   ├── Scripts/
│   │   ├── Core/          # 游戏核心
│   │   ├── Protocol/      # 网络协议
│   │   ├── Client/        # 网络客户端
│   │   ├── Player/        # 输入处理
│   │   ├── Presentation/  # 角色表现
│   │   ├── Combat/        # 战斗表现
│   │   ├── Camera/        # 相机系统
│   │   ├── UI/            # 界面系统
│   │   └── Editor/        # 编辑器工具
│   ├── Animations/        # 动画资源
│   ├── Prefabs/           # 预制体
│   ├── Materials/         # 材质
│   ├── Scenes/            # 场景
│   └── Config/            # 配置文件
├── server/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/      # Java 源码
│       │   ├── proto/     # Protobuf 定义
│       │   └── resources/ # 配置文件
│       └── test/          # 单元测试
├── Packages/              # Unity 包管理
├── ProjectSettings/       # Unity 项目设置
├── Dockerfile
└── docker-compose.yml
```

## 八、关键设计决策

### 8.1 状态同步而非帧同步

服务端以固定频率广播完整状态快照
客户端无需预测与回滚
实现简单，适合动作游戏原型

### 8.2 三层反作弊架构

Layer 1：实时检测
- 检查移动速度异常
- 操作频率限制

Layer 2：逻辑校验
- 伤害数值校验
- 技能冷却校验

Layer 3：回放分析
- 战斗结束后 LLM 分析
- 生成作弊报告

### 8.3 纯表现客户端

客户端不存储权威数值
所有数据从最新帧快照实时读取

好处：
- 杜绝客户端作弊
- 简化客户端逻辑
- 表现与判定完全分离

### 8.4 对象池优化

VFXEffectManager 使用对象池
减少运行时 GC 压力
支持自动扩展与生命周期管理

## 九、扩展指南

### 9.1 添加新技能

服务端：
1. 在 SkillConfig 添加配置
2. 在 CombatSystem 实现效果
3. 更新消息协议（如需新字段）

客户端：
1. 添加技能动画到 Animator
2. 配置 VFX 预制体到 VFXEffectManager
3. 在 PlayerInputHandler 绑定按键

### 9.2 添加新怪物

服务端：
1. 在 MonsterConfig 添加属性
2. 实现新的 AIController（可选）
3. 在 BattleRoom.spawnMonster 中配置

客户端：
1. 导入怪物模型与动画
2. 创建预制体并挂载 CharacterPresentation
3. 配置自动匹配索引

### 9.3 修改网络协议

1. 修改 `server/src/main/proto/*.proto`
2. 运行 `mvn protobuf:compile` 生成 Java 代码
3. 同步修改 `Assets/Scripts/Protocol/` 下的 C# 结构
4. 更新 BinarySerializer 的读写逻辑
5. 在 NetworkClient 注册新消息处理器

## 十、调试与开发

### 10.1 服务端日志

日志位于 `server/logs/`
或通过 Docker Volume 挂载查看

### 10.2 客户端调试

Unity Console 查看网络日志
仅 UNITY_EDITOR 下输出详细日志

### 10.3 网络延迟模拟

修改 NetworkClient 中的 m_serverHost
使用 Clumsy 或 Network Link Conditioner
模拟高延迟与丢包环境

## 十一、版本历史

当前版本：1.0.0-SNAPSHOT

Unity 版本：2022 LTS
Java 版本：17
Netty 版本：4.1.108.Final

## 十二、注意事项

服务端必须先于客户端启动
否则客户端无法连接

客户端与服务端协议版本必须兼容
当前协议版本号为 10

所有战斗判定以服务端为准
客户端数值仅用于 UI 展示

Docker 部署时确保端口映射正确
默认暴露 9090 端口

## 十三、详细类与函数说明

### 13.1 客户端核心类详解

**GameManager**
- `Awake()` 初始化单例，标记 DontDestroyOnLoad
- `Start()` 设置初始状态为 Playing
- `ChangeGameState(GameState newState)` 切换状态，处理 timeScale，广播事件
- `TogglePause()` 在 Playing 与 Paused 间切换
- `RestartGame()` 重新加载当前场景
- `QuitGame()` 退出游戏或停止编辑器播放
- 属性：`IsPlaying` / `IsGameFrozen`

**EventManager**
- `OnGameStateChanged` 状态变更事件
- `OnBattleStart` / `OnBattleEnd` 战斗起止事件
- `TriggerOnGameStateChanged(newState)` 触发状态变更

**SceneBootstrap**
- `Awake()` 依次初始化 GameManager、EventManager、Camera
- `InitializeGameManager()` 查找或创建 GameManager
- `InitializeEventManager()` 查找或创建 EventManager
- `InitializeCamera()` 为主相机添加 CameraRig 组件

**Singleton<T>**
- `Instance` 全局访问点
- `Awake()` 处理重复实例销毁逻辑

**PlayerInputHandler**
- `Update()` 每帧读取输入，处理暂停键特殊逻辑
- `ReadMovementInput()` 读取 Horizontal/Vertical 轴向，对角线归一化
- `ReadActionInputs()` 读取 Jump/Attack/Skill/Ultimate/Dodge/LockOn/Tab
- `ReadSystemInputs()` 读取 Escape 暂停键
- `EnableInput()` / `DisableInput()` 输入锁定
- `ConsumeXXX()` 系列方法清零触发状态
- `ClearAllInputs()` 清空所有输入

**NetworkClient**
- `Awake()` 初始化单例
- `Start()` 创建 WebSocketClient，注册消息处理器
- `Update()` 驱动 WebSocket 更新、心跳计时、自动重连、请求超时检测
- `Connect()` 异步连接服务端
- `Disconnect()` 断开连接
- `Login(name, configId, token)` 发送登录请求
- `JoinRoom(playerId, roomId)` 发送加入房间请求
- `SendPlayerAction(entityId, frameIndex, actions)` 发送玩家操作
- `SendMessage(messageId, message)` 通用消息发送
- `RegisterHandler(messageId, handler)` 注册消息回调
- 事件：`OnConnected` / `OnDisconnected` / `OnLoginSuccess` / `OnLoginFailed` / `OnJoinRoomResp` / `OnBattleFrame` / `OnBattleStart` / `OnBattleEnd`

**WebSocketClient**
- `ConnectAsync(host, port, path)` 建立 WebSocket 连接，启动接收循环
- `DisconnectAsync()` 关闭连接，清理资源
- `SendAsync(data)` 发送二进制数据
- `ReceiveLoopAsync()` 后台线程接收循环，支持分片消息
- `Update()` 主线程消费接收队列
- `SetAuthenticated()` / `ResetAuth()` 认证状态切换
- 事件：`OnConnected` / `OnDisconnected` / `OnMessageReceived`

**FrameSnapshotManager**
- `Awake()` 初始化单例
- `Start()` 订阅 NetworkClient 战斗事件
- `HandleBattleFrame(frame)` 接收帧快照，更新内部缓冲
- `HandleBattleStart(notify)` / `HandleBattleEnd(notify)` 战斗起止处理
- `TryGetSnapshot(entityId, out snapshot)` 查询角色快照
- `GetPosition(entityId)` / `GetState(entityId)` / `GetCurrentHealth(entityId)` 查询接口
- `ConsumeDamageEvents()` / `ConsumeVFXEvents()` 消费并清空 pending 事件
- `GetAllEntityIds()` 获取所有实体 ID
- 属性：`CurrentFrameIndex` / `IsBattleActive`

**CharacterPresentation**
- `Initialize(entityId, isLocalPlayer)` 绑定实体 ID
- `LateUpdate()` 自动匹配 entityId，更新位置与动画
- `UpdateRemotePosition()` 对远程玩家使用 Lerp/Slerp 插值
- `UpdateAnimationState(snapshotMgr)` 根据状态切换 Animator 参数
- 属性：`EntityId` / `IsLocalPlayer`

**BattleEventConsumer**
- `Awake()` 初始化单例
- `LateUpdate()` 消费 FrameSnapshotManager 中的伤害与特效事件
- `ShowDamageNumber(position, damage, isCritical, element)` 播放伤害特效

**VFXEffectManager**
- `Awake()` 初始化单例，构建对象池
- `Update()` 更新跟随特效位置，回收超时特效
- `PlayEffect(type, position, rotation, followTarget)` 世界坐标播放
- `PlayEffectTinted(type, position, rotation, tintColor)` 带颜色染色播放
- `PlayEffectFollow(type, target, localOffset, rotation)` 跟随目标播放
- `StopEffect(instance)` / `StopEffectsOnTarget(target)` / `StopAllOfType(type)` 停止特效
- `ClearAll()` 清除所有活跃特效
- `HasEffect(type)` 查询是否配置
- 私有方法：`GetFromPool()` / `Despawn()` / `ApplyTint()` / `ResetTint()` / `DetectMaxDuration()`

**CameraRig**
- `Start()` 查找 Player 标签目标
- `LateUpdate()` 处理输入、更新位置、更新旋转
- `HandleInput()` 鼠标右键控制 yaw/pitch
- `HandleLockedRotation()` 锁定目标时自动计算朝向
- `UpdatePosition()` 平滑阻尼跟随，球体碰撞检测
- `UpdateRotation()` 朝向目标点，锁定模式下看向中点
- 属性：`Target` / `Yaw` / `Pitch`

**UIManager**
- `Awake()` 初始化单例
- `Start()` 绑定面板引用，初始隐藏
- `OnGameStateChanged(newState)` 根据状态切换面板
- `ShowPanel(panel)` / `HideTopPanel()` / `HideAllPanels()` 面板栈操作
- `OnStartGame()` / `OnPause()` / `OnResume()` / `OnOpenSettings()` / `OnCloseSettings()` / `OnReturnToMenu()` / `OnQuitGame()` 按钮回调

### 13.2 服务端核心类详解

**Bootstrap**
- `main(String[] args)` 解析端口参数，初始化所有组件，启动服务器，添加关闭钩子

**WebSocketServer**
- `WebSocketServer(port, connectionManager, handlerRegistry)` 构造器
- `start()` 配置 Netty ServerBootstrap，绑定端口，启动服务
- `shutdown()` 优雅关闭 channel 与 EventLoopGroup

**WebSocketServerHandler**
- `channelRead0(ctx, msg)` 读取 WebSocket 二进制帧，解析 MessageWrapper，路由到 Handler
- `channelActive(ctx)` / `channelInactive(ctx)` 连接建立与断开处理
- `userEventTriggered(ctx, evt)` 处理 IdleStateEvent 心跳超时

**HandlerRegistry**
- `register(handler)` 注册消息处理器
- `dispatch(ctx, wrapper)` 根据 MessageId 分发到对应处理器

**IMessageHandler<T>**
- `getMessageId()` 返回处理的消息类型
- `handle(ctx, wrapper, payload)` 处理消息逻辑

**ConnectionManager**
- `addSession(channel, session)` 添加会话
- `removeSession(channel)` 移除会话
- `getSession(channel)` 获取会话
- `broadcast(message)` 广播消息到所有连接

**GameSession**
- `send(data)` 发送二进制数据
- `isActive()` 检查连接是否活跃
- `nextSequenceId()` 生成递增序列号
- 属性：`playerId` / `isAuthenticated`

**BattleRoom**
- `BattleRoom(roomId)` 构造器，初始化所有子系统
- `addPlayer(character, session)` 添加玩家，分配 entityId
- `removePlayer(entityId)` 移除玩家
- `spawnMonster(config, isBoss)` 生成怪物，设置初始位置
- `startBattle()` 启动战斗，启动 FrameScheduler
- `endBattle()` 结束战斗，触发反作弊分析与 LLM 分析
- `closeRoom()` 关闭房间
- `submitPlayerAction(entityId, actionType, moveX, moveZ, skillId, targetEntityId)` 提交玩家操作，实时反作弊检测
- `executeFrame(frameIndex, deltaTime)` 帧执行接口：处理操作 → AI更新 → Buff更新 → 资源回收 → 反作弊追踪 → 生成快照 → 广播帧 → 检查结束
- `processPendingActions(deltaTime)` 处理 pending 操作队列
- `executePlayerAttack(attacker)` 执行玩家攻击，命中检测，反作弊校验
- `buildFrameSnapshot(frameIndex)` 构建帧快照
- `broadcastFrame(frame)` 序列化并广播帧到所有玩家
- `checkBattleEnd()` 检查玩家全灭或怪物全灭
- `findPlayer(entityId)` 查找 RoomPlayer

**FrameScheduler**
- `FrameScheduler(executor)` / `FrameScheduler(executor, frameIntervalMs)` 构造器
- `start()` 启动调度线程
- `stop()` 停止调度线程
- `runLoop()` 固定时间步长循环，执行帧，处理掉帧补偿
- `isRunning()` 查询运行状态

**CombatSystem**
- `CombatSystem()` 构造器，初始化 DamagePipeline 与 HitDetectionSystem
- `setBuffEngineResolver(resolver)` 注入 BuffEngine 解析器
- `executeAttack(attacker, target, skillMultiplier, attackElement)` 单体攻击
- `executeAreaAttack(attacker, center, radius, candidates, alreadyHit, skillMultiplier, element)` 范围攻击
- `applyBuff(target, buffConfig, source)` 添加 Buff
- `updateBuffs(target, deltaTime)` 更新 Buff

**DamagePipeline**
- `DamagePipeline()` 默认构造器，注册4步管线
- `DamagePipeline(customSteps)` 自定义步骤构造器
- `calculate(input, context)` 执行完整伤害计算
- `calculateSimple(input, attackPower, defense, attackerEntityId, targetEntityId)` 简化计算
- `getSteps()` 获取步骤列表

**IDamageStep**
- `execute(damageInfo, context)` 执行单步计算，返回当前伤害值

**HitDetectionSystem**
- `detectHits(attacker, center, radius, candidates, alreadyHit)` 范围命中检测

**BuffEngine**
- `BuffEngine(owner)` 构造器
- `addBuff(config, source)` 添加 Buff，处理叠加规则
- `removeBuff(buff)` / `removeBuffByName(name)` / `removeBuffByType(type)` 移除 Buff
- `clearAllBuffs()` 清空所有 Buff
- `update(deltaTime)` 每帧更新所有 Buff，处理过期
- `getActiveBuffs()` / `findBuffByName(name)` / `hasBuff(name)` 查询接口

**EnemyAIController**
- `update(deltaTime)` AI 决策入口
- `changeState(type)` 切换 FSM 状态
- `getCurrentState()` 获取当前状态

**BossAIController（继承 EnemyAIController）**
- 复用父类状态机
- 可扩展 Boss 专属行为

**Character（抽象基类）**
- `takeDamage(damage)` 扣减生命值
- `isDead()` 判断是否死亡
- `getPosition()` / `setPosition(pos)` 位置访问
- `getRotation()` / `setRotation(rot)` 旋转访问
- `getState()` / `setState(state)` 状态访问
- `getStats()` 获取属性
- `getBuffs()` 获取 Buff 列表
- `getEntityId()` / `getConfigId()` / `getElementType()` 基础信息
- `isInvincible()` / `getTargetEntityId()` / `getComboStep()` 战斗信息

**PlayerCharacter（继承 Character）**
- `getPlayerId()` / `getSessionKey()` 玩家信息

**MonsterCharacter（继承 Character）**
- `getMonsterConfigId()` 怪物配置 ID

**CharacterStats**
- `getMaxHealth()` / `getCurrentHealth()` / `setCurrentHealth(value)` 生命
- `getEffectiveAttackPower()` / `getEffectiveDefense()` / `getEffectiveMoveSpeed()` 有效属性
- `getEffectiveCriticalRate()` / `getCriticalDamageMultiplier()` 暴击相关
- `getDamageTakenMultiplier()` 受伤倍率
- `resetAllModifiers()` 重置所有属性修饰

**CheatDetector**
- `CheatDetector(deltaTime)` 构造器
- `checkRealtime(entityId, actionType, moveX, moveZ, timestamp)` Layer 1 实时检测
- `checkLogic(attacker, target, damage)` Layer 2 逻辑校验
- `updatePlayerPosition(entityId, position)` 更新玩家位置用于追踪
- `getTotalViolationScore()` 获取总违规分数
- `getIncidents()` 获取作弊事件列表

**LLMService（接口）**
- `generateBattleSummary(context)` 生成战斗总结
- `analyzeDifficulty(context)` 分析难度并给出建议
- `analyzeCheatPatterns(context)` 分析作弊模式

**LocalLLMService（实现 LLMService）**
- 本地 LLM 调用实现

### 13.3 协议结构详解

**MessageWrapper（消息信封）**
- 帧格式：`[MessageId(4)] [ProtocolVersion(4)] [SequenceId(8)] [PayloadLen(4)] [Payload]`
- `ToByteArray()` 序列化为字节数组
- `FromByteArray(data, offset, length)` 从字节数组解析
- `ParseFrame(frameData)` 从 WebSocket 帧解析
- `Wrap(messageId, sequenceId, protocolVersion, payload)` 构建包装器

**BinarySerializer**
- 大端字节序
- `WriteInt/WriteLong/WriteFloat/WriteBool/WriteString` 写入方法
- `ReadInt/ReadLong/ReadFloat/ReadBool/ReadString` 读取方法
- `StringSize(value)` 计算字符串序列化后大小

**PlayerActionData（C# 结构体）**
- 字段：`ActionType` / `ActionId` / `MoveX` / `MoveZ` / `SkillId` / `TargetElement` / `TargetEntityId` / `Timestamp`
- `SerializedSize` 固定大小 30 字节
- `Serialize(buffer, ref offset)` / `Deserialize(buffer, ref offset)` 序列化反序列化

**CharacterSnapshot（C# 结构体）**
- 字段：`EntityId` / `ConfigId` / `EntityType` / `Position` / `Rotation` / `State` / `Stats` / `Buffs` / `ComboStep` / `IsInvincible` / `TargetEntityId`
- 变长序列化（Buffs 数组长度不定）

## 十四、模块间调用关系

### 14.1 客户端调用链

**启动流程**
```
SceneBootstrap.Awake()
  ├── GameManager.Awake()           (单例初始化)
  ├── EventManager.Awake()          (单例初始化)
  └── CameraRig.Start()             (查找 Player 标签)
```

**网络连接流程**
```
NetworkTestComponent.Start()
  └── NetworkClient.Connect()
        └── WebSocketClient.ConnectAsync()
              ├── 建立 WebSocket 连接
              └── 启动 ReceiveLoopAsync() 后台线程
```

**登录流程**
```
NetworkClient.Login(name, configId, token)
  ├── SendMessage(LoginReq)
  └── WebSocketClient.SendAsync()

WebSocketClient.ReceiveLoopAsync() 收到响应
  └── NetworkClient.OnRawMessageReceived()
        └── HandleLoginResp()
              ├── 设置 m_wasAuthenticated = true
              └── OnLoginSuccess?.Invoke(resp)
```

**输入到网络发送流程**
```
PlayerInputHandler.Update()           (每帧读取输入)
  └── NetworkInputBridge.Update()     (将输入转为操作)
        └── NetworkClient.SendPlayerAction()
              └── SendMessage(PlayerActionReq)
                    └── WebSocketClient.SendAsync()
```

**服务端帧到表现流程**
```
WebSocketClient.ReceiveLoopAsync()    (后台线程接收)
  └── m_receiveQueue.Enqueue(data)

NetworkClient.Update()                (主线程消费)
  └── WebSocketClient.Update()
        └── OnMessageReceived?.Invoke(data)
              └── NetworkClient.OnRawMessageReceived()
                    └── MessageWrapper.ParseFrame()
                          └── HandleBattleFrame()
                                └── FrameSnapshotManager.HandleBattleFrame()

CharacterPresentation.LateUpdate()    (从帧快照读取)
  ├── UpdateRemotePosition()          (Lerp 插值位置)
  └── UpdateAnimationState()          (Animator 驱动)

BattleEventConsumer.LateUpdate()      (消费事件)
  ├── ConsumeDamageEvents()
  └── ConsumeVFXEvents()
        └── VFXEffectManager.PlayEffect()
```

**游戏状态切换流程**
```
PlayerInputHandler.Update()
  └── GameManager.TogglePause()
        └── GameManager.ChangeGameState(Paused)
              ├── Time.timeScale = 0
              └── EventManager.TriggerOnGameStateChanged()
                    └── UIManager.OnGameStateChanged()
                          └── ShowPanel(_pausePanel)
```

### 14.2 服务端调用链

**启动流程**
```
Bootstrap.main(args)
  ├── ConnectionManager 实例化
  ├── AuditLoggerImpl 实例化
  ├── HandlerRegistry 实例化
  │     ├── register(LoginHandler)
  │     ├── register(HeartbeatHandler)
  │     ├── register(JoinRoomHandler)
  │     └── register(PlayerActionHandler)
  └── WebSocketServer.start()
        ├── NioEventLoopGroup 创建
        ├── ServerBootstrap 配置 Pipeline
        └── ChannelFuture.bind(port).sync()
```

**连接建立流程**
```
WebSocketServerHandler.channelActive()
  └── ConnectionManager.addSession()
```

**登录流程**
```
WebSocketServerHandler.channelRead0()
  └── HandlerRegistry.dispatch()
        └── LoginHandler.handle()
              ├── 解析 LoginReq
              ├── 认证校验
              ├── Session.setAuthenticated(true)
              └── 发送 LoginResp
```

**加入房间流程**
```
LoginHandler.handle() 成功后
  或客户端发送 JoinRoomReq
  └── JoinRoomHandler.handle()
        ├── RoomManager.createRoom() / getRoom()
        └── BattleRoom.addPlayer(character, session)
              ├── 分配 entityId
              ├── 创建 BuffEngine
              └── 加入 entityMap
```

**战斗帧循环流程**
```
FrameScheduler.runLoop()              (独立线程，每 20ms)
  └── BattleRoom.executeFrame(frameIndex, deltaTime)
        ├── processPendingActions()   (处理玩家操作队列)
        │     ├── 反作弊实时检测
        │     └── 更新角色位置/状态
        ├── RoomMonster.update()      (AI 更新)
        │     └── EnemyAIController.update()
        │           └── 当前状态.update()
        ├── BuffEngine.update()       (Buff 更新)
        ├── ResourceRecycler.recycleDeadMonsters()
        ├── ResourceRecycler.recycleOfflinePlayers()
        ├── cheatDetector.updatePlayerPosition()
        ├── buildFrameSnapshot()      (生成帧快照)
        └── broadcastFrame()          (广播到所有玩家)
              └── BattleFrameSerializer.serialize()
                    └── GameSession.send()
```

**玩家攻击流程**
```
BattleRoom.processPendingActions()
  └── executePlayerAttack(attacker)
        ├── attacker.setState(ATTACK)
        ├── combatSystem.executeAreaAttack()
        │     ├── hitDetection.detectHits()   (范围命中检测)
        │     └── combatSystem.executeAttack() (逐个结算)
        │           ├── DamagePipeline.calculate()
        │           │     ├── BaseDamageStep.execute()
        │           │     ├── CriticalDamageStep.execute()
        │           │     ├── ElementDamageStep.execute()
        │           │     └── DefenseDamageStep.execute()
        │           └── target.takeDamage()
        └── cheatDetector.checkLogic()        (伤害异常检测)
```

**战斗结束流程**
```
BattleRoom.checkBattleEnd()
  └── BattleRoom.endBattle()
        ├── scheduler.stop()
        ├── cheatDetector.getTotalViolationScore()
        │     └── cheatResponseManager.submitIncident()
        └── llmService.generateBattleSummary()
              └── llmService.analyzeDifficulty()
```

## 十五、数据流向图

### 15.1 玩家输入到画面表现（完整链路）

```
键盘/鼠标输入
    │
    ▼
PlayerInputHandler.Update()
    │ (读取输入状态)
    ▼
NetworkInputBridge.Update()
    │ (组装 PlayerActionData)
    ▼
NetworkClient.SendPlayerAction()
    │ (序列化为 PlayerActionReq)
    ▼
WebSocketClient.SendAsync()
    │ (WebSocket 二进制帧)
    ▼
┌─────────────┐
│   Internet  │
└─────────────┘
    │
    ▼
WebSocketServer (Netty)
    │
    ▼
WebSocketServerHandler.channelRead0()
    │
    ▼
HandlerRegistry.dispatch()
    │
    ▼
PlayerActionHandler.handle()
    │ (解析操作)
    ▼
BattleRoom.submitPlayerAction()
    │ (实时反作弊检测)
    ▼
pendingActions 队列
    │
    ▼
FrameScheduler.runLoop()
    │ (每 20ms)
    ▼
BattleRoom.executeFrame()
    │ (处理 pendingActions)
    ▼
processPendingActions()
    │ (更新角色位置/状态)
    ▼
buildFrameSnapshot()
    │ (生成 BattleFrame)
    ▼
broadcastFrame()
    │ (序列化 BattleFrameNotify)
    ▼
GameSession.send()
    │ (WebSocket 二进制帧)
    ▼
┌─────────────┐
│   Internet  │
└─────────────┘
    │
    ▼
WebSocketClient.ReceiveLoopAsync()
    │
    ▼
m_receiveQueue.Enqueue()
    │
    ▼
NetworkClient.Update()
    │ (主线程消费)
    ▼
FrameSnapshotManager.HandleBattleFrame()
    │ (存储帧快照)
    ▼
├──────────────────┬──────────────────┤
│                  │                  │
▼                  ▼                  ▼
CharacterPresentation    BattleEventConsumer
.LateUpdate()            .LateUpdate()
│                        │
▼                        ▼
Animator.SetInteger()    VFXEffectManager.PlayEffect()
│                        │
▼                        ▼
画面更新（位置/动画）     粒子特效播放
```

### 15.2 伤害计算数据流

```
executePlayerAttack(attacker)
    │
    ▼
combatSystem.executeAreaAttack()
    │
    ├── hitDetection.detectHits()
    │   │ (基于距离范围检测)
    │   └── 返回命中目标列表
    │
    └── 遍历命中目标
        │
        ▼
    combatSystem.executeAttack()
        │
        ├── 构造 DamageInfo
        │   ├── skillMultiplier
        │   ├── attackElement
        │   ├── defenderElement
        │   └── hitPosition
        │
        ├── 构造 DamageContext
        │   ├── attackerAttackPower
        │   ├── targetDefense
        │   ├── criticalRate
        │   ├── criticalDamageMultiplier
        │   ├── damageTakenMultiplier
        │   ├── attackerEntityId
        │   └── targetEntityId
        │
        ├── DamagePipeline.calculate()
        │   │
        │   ├── BaseDamageStep.execute()
        │   │   └── damage = attackPower - defense * 0.5
        │   │
        │   ├── CriticalDamageStep.execute()
        │   │   ├── Random < criticalRate ? 暴击
        │   │   └── damage *= criticalDamageMultiplier
        │   │
        │   ├── ElementDamageStep.execute()
        │   │   ├── 查询元素克制表
        │   │   └── damage *= elementMultiplier
        │   │
        │   └── DefenseDamageStep.execute()
        │       └── damage *= damageTakenMultiplier
        │
        ├── target.takeDamage(finalDamage)
        │   └── currentHealth -= damage
        │
        └── 返回 HitResult
            ├── isHit = true
            ├── targetEntityId
            ├── damage
            ├── isCritical
            └── element
```

### 15.3 帧快照生成与广播数据流

```
BattleRoom.buildFrameSnapshot(frameIndex)
    │
    ├── 遍历 RoomPlayer
    │   │
    │   └── frame.addCharacterSnapshot(playerCharacter, 0)
    │       │
    │       ├── 收集 BuffSnapshots
    │       ├── 构建 CharacterStatsSnapshot
    │       └── 构建 CharacterSnapshot
    │           ├── entityId, configId, entityType=0
    │           ├── position, rotation, state
    │           ├── stats (maxHealth, currentHealth, attack, defense...)
    │           ├── buffs 列表
    │           ├── comboStep, isInvincible, targetEntityId
    │
    ├── 遍历 RoomMonster
    │   │
    │   └── frame.addCharacterSnapshot(monsterCharacter, type)
    │       └── 同上，entityType = 1 或 2
    │
    └── 返回 BattleFrame

BattleRoom.broadcastFrame(frame)
    │
    ├── payload = BattleFrameSerializer.serialize(frame, protocolVersion)
    │   │
    │   ├── 写入 frameIndex, timestamp, roomId
    │   ├── 写入 characterCount
    │   ├── 逐个序列化 CharacterSnapshot
    │   ├── 写入 damageEventCount
    │   ├── 逐个序列化 DamageNumberEvent
    │   ├── 写入 vfxEventCount
    │   └── 逐个序列化 VFXEvent
    │
    └── 遍历 RoomPlayer
        │
        └── 若 session.active
            ├── MessageHelper.wrap(BATTLE_FRAME_NOTIFY, seqId, payload)
            └── session.send(wrapper)
                └── WebSocketServerHandler.writeAndFlush()
```

### 15.4 反作弊检测数据流

```
玩家操作输入
    │
    ▼
BattleRoom.submitPlayerAction()
    │
    ├── Layer 1: 实时检测
    │   │
    │   └── cheatDetector.checkRealtime()
    │       ├── 检查移动速度是否超过阈值
    │       ├── 检查操作频率是否异常
    │       └── 若异常：记录 CheatIncident，拒绝操作
    │
    └── 通过检测 → 加入 pendingActions 队列

战斗结算时
    │
    ▼
executePlayerAttack()
    │
    └── Layer 2: 逻辑校验
        │
        └── cheatDetector.checkLogic()
            ├── 检查伤害数值是否在合理范围
            ├── 检查攻击距离是否异常
            └── 若异常：记录 CheatIncident

BattleRoom.endBattle()
    │
    └── Layer 3: 回放分析
        │
        ├── cheatDetector.getTotalViolationScore()
        │   └── 汇总所有 CheatIncident
        │
        ├── cheatResponseManager.submitIncident()
        │   └── 根据严重程度生成响应
        │
        └── llmService.analyzeCheatPatterns()
            └── 生成自然语言作弊分析报告
```

## 十六、关键性能考量

### 16.1 网络优化

帧快照延迟序列化
服务端只序列化一次
多玩家共享同一 payload

帧缓冲设计
客户端缓冲最近 3 帧
平滑网络抖动

消息分片支持
WebSocketClient 支持分片消息重组
避免大数据包截断

### 16.2 内存优化

对象池
VFXEffectManager 管理特效对象池
减少运行时 GC

帧事件上限
DamageEvents 与 VFXEvents 各限制 500 条
防止内存无限增长

CopyOnWriteArrayList
BattleRoom 中 players 与 monsters 使用
避免帧循环中的并发修改异常

### 16.3 计算优化

帧循环异常隔离
AI 更新、Buff 更新均包裹 try-catch
单实体异常不影响整帧

帧追赶限制
FrameScheduler 最大追赶 5 帧
掉帧过多时重置计时器

碰撞检测
CameraRig 使用 SphereCast 而非 RayCast
减少物理计算开销

## 十七、设计模式梳理

### 17.1 单例模式 (Singleton)

使用位置：
- GameManager / EventManager / UIManager
- NetworkClient / FrameSnapshotManager
- BattleEventConsumer / VFXEffectManager

实现方式：
C# 端继承 Singleton<T> 泛型基类
Java 端使用静态实例或依赖注入

作用：
全局唯一访问点
简化跨模块通信

注意：
单例过多会增加耦合
本项目中主要用于管理器类

### 17.2 状态模式 (State)

使用位置：
- GameManager.GameState 枚举
- EnemyState FSM 状态机
- CharacterState 角色状态

实现方式：
GameManager 用枚举 + switch 实现
AI 系统用抽象基类 + 多态实现

作用：
清晰划分不同行为阶段
避免大量条件判断

### 17.3 管线模式 (Pipeline)

使用位置：
DamagePipeline 伤害计算

实现方式：
IDamageStep 接口定义单步操作
DamagePipeline 顺序执行所有步骤
每步输出作为下步输入

作用：
伤害计算步骤可插拔
便于扩展新伤害类型

### 17.4 对象池模式 (Object Pool)

使用位置：
VFXEffectManager 特效管理

实现方式：
Dictionary<VFXEffectType, Queue<GameObject>>
初始化时预创建对象
使用时出队，回收时入队

作用：
减少运行时实例化开销
降低 GC 频率

### 17.5 策略模式 (Strategy)

使用位置：
- Buff 叠加规则 (BuffStackingRule)
- 目标选择策略 (TargetSelector)
- 元素克制计算

实现方式：
枚举或接口定义策略
运行时根据配置选择策略

作用：
行为变化独立于使用者
支持运行时切换策略

### 17.6 观察者模式 (Observer)

使用位置：
- EventManager 事件总线
- NetworkClient 消息事件
- WebSocketClient 连接事件

实现方式：
C# 事件委托 (event Action)
Java 回调接口

作用：
发布订阅解耦模块
一对多通知机制

### 17.7 工厂模式 (Factory)

使用位置：
- BuffFactory 创建 Buff 实例
- MessageWrapper 创建消息
- CreateMessage 工厂方法

实现方式：
静态工厂方法
根据类型参数创建实例

作用：
封装创建逻辑
集中管理对象生成

### 17.8 门面模式 (Facade)

使用位置：
- CombatSystem 协调战斗子系统
- NetworkClient 封装 WebSocket 细节

实现方式：
提供高层简化接口
内部委托给子系统

作用：
降低外部使用复杂度
隐藏内部实现细节

## 十八、潜在问题与技术债务

### 18.1 网络层

WebSocketClient 使用 async void
异常可能无法被正确捕获
建议改为返回 Task 并 await

NetworkClient.SendMessage 为 async void
存在 Fire-and-Forget 风险
建议添加发送失败回调

帧快照无压缩
大量角色时带宽占用高
可考虑 Delta 压缩或增量同步

### 18.2 战斗系统

伤害公式硬编码
BaseDamageStep 中 defense * 0.5
建议抽取到配置文件中

元素克制表未完整实现
ElementDamageStep 可能缺少配置
需要补充 ElementConfig 数据

命中检测仅基于距离
无碰撞体或射线检测
可能导致穿墙攻击

### 18.3 AI 系统

SimplePathfinder 过于简单
仅做线性移动，无避障
建议集成 NavMesh 或 A* 寻路

AI 状态机缺少过渡条件校验
可能从任意状态切换到任意状态
建议增加状态转换表限制

### 18.4 反作弊

CheatDetector 阈值硬编码
AntiCheatConfig 未完全使用
建议全部参数化配置

LLM 服务异常时无降级策略
LocalLLMService 失败会导致分析中断
建议添加 try-catch 并记录日志

### 18.5 客户端表现

CharacterPresentation 自动匹配索引
依赖实体顺序，不够稳定
建议显式分配 entityId

VFXEffectManager 染色使用 material
创建临时材质实例增加内存开销
建议改用 Shader Property 或 MaterialPropertyBlock

### 18.6 代码结构

BattleRoom 职责过多
同时管理玩家、怪物、帧循环、战斗结算
建议拆分为 RoomManager + BattleController

FrameSnapshotManager 同时承担
数据存储与事件消费职责
建议将事件消费移至独立组件

### 18.7 测试覆盖

客户端现有 14 个 C# 协议测试
核心逻辑（如 BinarySerializer）仍可补充更多覆盖

服务端已有端到端集成、并发边界与压测测试
新增 WebSocket 压测工具与 benchmark 报告

## 十九、扩展路线图

### 近期（1-2 周）

- 补充元素克制配置数据
- 优化帧快照序列化大小
- 添加客户端单元测试

### 中期（1 个月）

- 集成 A* 寻路替换 SimplePathfinder
- 实现技能配置化加载
- 添加战斗回放查看器

### 远期（3 个月）

- 支持多房间并发战斗
- 添加排行榜与持久化存储
- 实现客户端预测与回滚

## 二十、快速参考索引

### 20.1 客户端核心文件路径

| 文件路径 | 职责 |
| --- | --- |
| Assets/Scripts/Core/GameManager.cs | 全局游戏状态机 |
| Assets/Scripts/Core/EventManager.cs | 全局事件总线 |
| Assets/Scripts/Core/SceneBootstrap.cs | 场景初始化引导 |
| Assets/Scripts/Core/Singleton.cs | 单例泛型基类 |
| Assets/Scripts/Core/SaveManager.cs | 本地存档管理 |
| Assets/Scripts/Protocol/Messages.cs | 网络消息定义 |
| Assets/Scripts/Protocol/Enums.cs | 协议枚举定义 |
| Assets/Scripts/Protocol/DataStructures.cs | 网络数据结构 |
| Assets/Scripts/Protocol/BinarySerializer.cs | 二进制序列化 |
| Assets/Scripts/Protocol/MessageWrapper.cs | 消息信封封装 |
| Assets/Scripts/Client/Network/NetworkClient.cs | 网络客户端主类 |
| Assets/Scripts/Client/Network/WebSocketClient.cs | WebSocket 底层实现 |
| Assets/Scripts/Client/Network/FrameSnapshotManager.cs | 帧快照管理 |
| Assets/Scripts/Player/PlayerInputHandler.cs | 输入采集处理 |
| Assets/Scripts/Player/NetworkInputBridge.cs | 输入网络桥接 |
| Assets/Scripts/Presentation/CharacterPresentation.cs | 角色表现驱动 |
| Assets/Scripts/Presentation/BattleEventConsumer.cs | 战斗事件消费 |
| Assets/Scripts/Combat/VFXEffectManager.cs | 视觉特效管理 |
| Assets/Scripts/Combat/VFXEffectType.cs | 特效类型枚举 |
| Assets/Scripts/Camera/CameraRig.cs | 第三人称相机 |
| Assets/Scripts/Camera/LockOnSystem.cs | 目标锁定系统 |
| Assets/Scripts/UI/UIManager.cs | UI 面板管理 |
| Assets/Scripts/UI/UIBase.cs | UI 面板基类 |
| Assets/Scripts/UI/Panels/MainMenuPanel.cs | 主菜单面板 |
| Assets/Scripts/UI/Panels/PausePanel.cs | 暂停面板 |
| Assets/Scripts/UI/Panels/SettingsPanel.cs | 设置面板 |

### 20.2 服务端核心文件路径

| 文件路径 | 职责 |
| --- | --- |
| server/src/main/java/com/actiongame/server/Bootstrap.java | 服务器启动入口 |
| server/src/main/java/com/actiongame/server/constant/GameConstants.java | 全局常量配置 |
| server/src/main/java/com/actiongame/server/net/WebSocketServer.java | Netty 服务器 |
| server/src/main/java/com/actiongame/server/net/WebSocketServerHandler.java | 业务消息分发 |
| server/src/main/java/com/actiongame/server/net/handler/HandlerRegistry.java | 处理器注册中心 |
| server/src/main/java/com/actiongame/server/net/handler/IMessageHandler.java | 处理器接口 |
| server/src/main/java/com/actiongame/server/net/handler/LoginHandler.java | 登录处理器 |
| server/src/main/java/com/actiongame/server/net/handler/HeartbeatHandler.java | 心跳处理器 |
| server/src/main/java/com/actiongame/server/net/handler/JoinRoomHandler.java | 加入房间处理器 |
| server/src/main/java/com/actiongame/server/net/handler/PlayerActionHandler.java | 玩家操作处理器 |
| server/src/main/java/com/actiongame/server/net/session/ConnectionManager.java | 连接管理 |
| server/src/main/java/com/actiongame/server/net/session/GameSession.java | 玩家会话 |
| server/src/main/java/com/actiongame/server/room/BattleRoom.java | 战斗房间核心 |
| server/src/main/java/com/actiongame/server/room/RoomManager.java | 房间管理 |
| server/src/main/java/com/actiongame/server/room/FrameScheduler.java | 帧调度器 |
| server/src/main/java/com/actiongame/server/room/FrameExecutor.java | 帧执行接口 |
| server/src/main/java/com/actiongame/server/room/BattleFrame.java | 帧快照数据 |
| server/src/main/java/com/actiongame/server/room/BattleFrameSerializer.java | 帧序列化 |
| server/src/main/java/com/actiongame/server/room/ResourceRecycler.java | 资源回收 |
| server/src/main/java/com/actiongame/server/battle/combatsystem/CombatSystem.java | 战斗系统主类 |
| server/src/main/java/com/actiongame/server/battle/pipeline/DamagePipeline.java | 伤害计算管线 |
| server/src/main/java/com/actiongame/server/battle/pipeline/IDamageStep.java | 伤害步骤接口 |
| server/src/main/java/com/actiongame/server/battle/combatsystem/HitDetectionSystem.java | 命中检测 |
| server/src/main/java/com/actiongame/server/battle/buffengine/BuffEngine.java | Buff 引擎 |
| server/src/main/java/com/actiongame/server/battle/buffengine/BuffFactory.java | Buff 工厂 |
| server/src/main/java/com/actiongame/server/ai/decision/EnemyAIController.java | 敌人 AI |
| server/src/main/java/com/actiongame/server/ai/decision/BossAIController.java | Boss AI |
| server/src/main/java/com/actiongame/server/ai/fsm/EnemyState.java | 状态基类 |
| server/src/main/java/com/actiongame/server/ai/fsm/EnemyStateType.java | 状态枚举 |
| server/src/main/java/com/actiongame/server/domain/character/Character.java | 角色基类 |
| server/src/main/java/com/actiongame/server/domain/character/PlayerCharacter.java | 玩家角色 |
| server/src/main/java/com/actiongame/server/domain/character/MonsterCharacter.java | 怪物角色 |
| server/src/main/java/com/actiongame/server/domain/character/CharacterStats.java | 角色属性 |
| server/src/main/java/com/actiongame/server/anticheat/CheatDetector.java | 反作弊检测器 |
| server/src/main/java/com/actiongame/server/anticheat/AntiCheatConfig.java | 反作弊配置 |
| server/src/main/java/com/actiongame/server/llm/LLMService.java | LLM 服务接口 |
| server/src/main/java/com/actiongame/server/llm/LocalLLMService.java | 本地 LLM 实现 |

### 20.3 配置与构建文件路径

| 文件路径 | 职责 |
| --- | --- |
| Packages/manifest.json | Unity 包依赖清单 |
| server/pom.xml | Maven 构建配置与依赖 |
| Dockerfile | 服务端容器镜像构建 |
| docker-compose.yml | Docker Compose 编排 |
| server/README.md | 服务端运行说明 |
| .env.example | 环境变量示例 |

### 20.4 测试文件路径

| 文件路径 | 职责 |
| --- | --- |
| server/src/test/java/com/actiongame/server/integration/EndToEndBattleFlowTest.java | 端到端战斗流程测试 |
| server/src/test/java/com/actiongame/server/integration/ConcurrencyAndEdgeCaseTest.java | 并发与边界测试 |
| server/src/test/java/com/actiongame/server/battle/combatsystem/ComboSystemTest.java | 连击系统测试 |
| server/src/test/java/com/actiongame/server/battle/buffengine/BuffEngineTest.java | Buff 引擎测试 |
| server/src/test/java/com/actiongame/server/ai/decision/EnemyAIControllerTest.java | AI 控制器测试 |
| server/src/test/java/com/actiongame/server/room/BattleRoomTest.java | 战斗房间测试 |
| server/src/test/java/com/actiongame/server/room/RoomManagerTest.java | 房间管理测试 |
| server/src/test/java/com/actiongame/server/anticheat/CheatDetectorTest.java | 反作弊测试 |
| server/src/test/java/com/actiongame/server/replay/ReplaySystemTest.java | 回放系统测试 |

## 二十一、结语

本文档基于对 ActionGameDemo
项目代码的全面分析编写

涵盖了客户端 Unity C# 代码
与服务端 Java 代码的核心逻辑

建议开发者在修改代码前
先阅读对应模块的架构红线

服务端权威、客户端表现
是本项目最重要的设计原则

如需更新本文档
请在重大架构变更后同步修订


