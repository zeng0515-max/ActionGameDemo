# ActionGameDemo

Realtime Combat Server
Java 权威战斗服务端
Unity 仅作为验证客户端

## 一、项目概述

ActionGameDemo 是一个实时对战服务端项目
定位为可复用的权威战斗后端
核心交付物在 `server/`

Unity 客户端不是项目主体
只负责输入采集、协议联调与表现验证

### 核心特性

- 50fps 帧同步状态广播
- WebSocket 二进制协议与 Netty Pipeline
- 服务端权威战斗计算
- 房间管理、帧调度与断线重连
- 可配置登录鉴权 token
- 跨节点房间匹配与负载分配
- MySQL 对局结果持久化与 Redis 房间状态缓存
- Prometheus 指标、健康检查与告警
- GitHub Actions 自动构建测试
- 三层实时反作弊检测
- AI 敌人 FSM 行为系统
- Boss 多阶段狂暴切换
- 战斗录像回放与结构化审计
- LLM 战斗复盘分析
- 192 个单元测试覆盖
- Docker 一键部署

### 架构红线

所有战斗判定逻辑
仅在 Java 服务端执行

客户端禁止修改权威数值
只负责输入采集与视觉表现

## 二、技术架构

```
┌─────────────────┐   WebSocket    ┌─────────────────┐
│   Unity Client  │ ◄────────────► │  Java Server    │
│   (C#)          │  Binary Proto  │  (Netty)        │
├─────────────────┤                ├─────────────────┤
│  Input Layer    │                │  Netty Pipeline │
│  Presentation   │                │  Message Router │
│  VFX / Camera   │                │  Battle Room    │
│  UI Panels      │                │  Combat System  │
│  Interpolation  │                │  AI + Buff      │
│  Reconciliation │                │  Anti-Cheat     │
└─────────────────┘                │  Replay + Audit │
                                   └─────────────────┘
```

## 三、技术栈

### 客户端（Unity）

- Unity 2022.3 LTS
- C# 9.0
- WebSocket 原生实现
- 自定义二进制序列化
- TextMeshPro UI
- NavMesh 导航

### 服务端（Java）

- JDK 17
- Maven 3.8+
- Netty 4.1.108
- Protobuf 3.25.3
- Gson 2.11.0
- SLF4J + Logback
- JUnit 5 + AssertJ
- Mockito 测试框架

### 部署

- Docker 多阶段构建
- Docker Compose 编排
- Kubernetes Kustomize 部署（base + dev/prod overlay）
- 约 200MB 镜像体积

## 四、项目结构

```
ActionGameDemo/
├── Assets/
│   ├── Scripts/
│   │   ├── Core/              # 核心管理器
│   │   ├── Protocol/          # 协议序列化
│   │   ├── Client/Network/    # WebSocket 网络层
│   │   ├── Player/            # 输入处理
│   │   ├── Presentation/      # 表现层驱动
│   │   ├── Combat/            # VFX 管理
│   │   ├── Camera/            # 相机与锁定
│   │   ├── UI/                # 界面面板
│   │   └── Tests/             # C# 单元测试
│   ├── Animations/            # Animator + 动画片段
│   ├── Prefabs/               # 角色/VFX/UI 预制体
│   ├── Scenes/                # 测试场景
│   └── Config/                # 数据配置资产
│
├── server/
│   ├── pom.xml
│   └── src/main/java/com/actiongame/server/
│       ├── Bootstrap.java           # 启动入口
│       ├── net/                      # Netty 网络层
│       ├── room/                     # 房间与帧循环
│       ├── battle/                   # 战斗核心系统
│       ├── ai/                       # AI 决策与 FSM
│       ├── domain/                   # 领域模型
│       ├── config/                   # 配置加载
│       ├── replay/                   # 战斗回放
│       ├── cheat/                    # 三层反作弊
│       ├── audit/                    # 审计日志
│       ├── llm/                      # LLM 分析模块
│       └── util/                     # 工具类
│
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── CODE_WIKI.md              # 详细技术文档
├── DOCKER_README.md          # Docker 部署指南
└── AUDIT_REPORT_PHASE1_7.md  # 代码审计报告
```

## 五、快速开始

### 环境要求

#### 服务端
- JDK 17+
- Maven 3.8+

#### 客户端
- Unity 2022.3 LTS 或更高

### 启动服务端

```bash
cd server
mvn clean package -DskipTests
java -jar target/action-game-server-1.0.0-SNAPSHOT.jar
```

默认端口：9090
连接地址：ws://127.0.0.1:9090/game

### Docker 部署

```bash
cp .env.example .env
docker-compose up -d
```

详细说明见 [DOCKER_README.md](DOCKER_README.md)

### 启动客户端

1. 用 Unity Hub 打开项目
2. 打开场景 `Assets/Scenes/Test_Stage0_Movement.unity`
3. 确认服务端已启动
4. 点击 Play 按钮

蓝色胶囊 = 玩家
红色胶囊 = 普通敌人
紫色胶囊 = Boss

## 六、通信协议

### 消息流程

```
Client                              Server
  │                                    │
  ├─ WebSocket Connect ──────────────►│
  │                                    │
  ├─ LoginReq (version) ─────────────►│
  │◄── LoginResp (version range) ─────┤
  │                                    │
  ├─ HeartbeatReq ───────────────────►│
  │◄── HeartbeatResp (rtt) ───────────┤
  │                                    │
  ├─ JoinRoomReq ────────────────────►│
  │◄── JoinRoomResp ──────────────────┤
  │◄── BattleStartNotify ─────────────┤
  │                                    │
  ├─ PlayerActionReq (20fps) ────────►│
  │◄── BattleFrameNotify (50fps) ─────┤
  │◄── PlayerActionResp ──────────────┤
  │                                    │
  │◄── BattleEndNotify ───────────────┤
```

### 消息 ID 分配

| 范围     | 类型       |
|---------|-----------|
| 1001-1099 | 登录协议   |
| 1101-1199 | 房间协议   |
| 1201-1299 | 战斗协议   |
| 2001-2099 | 系统协议   |

### 玩家操作类型

- MOVE  移动
- JUMP  跳跃
- ATTACK  普通攻击
- SKILL  技能
- ULTIMATE  大招
- DODGE  闪避

## 七、核心模块说明

### 战斗系统（服务端）

- 伤害流水线（5步策略模式）
  - 基础伤害计算
  - 暴击判定
  - 元素反应
  - 防御减伤
  - 最终结算

- Buff 引擎
  - 属性 Buff（攻/防/速）
  - 持续伤害（点燃/中毒）
  - 控制效果（冻结/麻痹）
  - 护盾吸收
  - 叠加规则：刷新/叠层/取最高

### AI 系统

- 有限状态机 FSM
  - Idle 待机
  - Patrol 巡逻
  - Chase 追击
  - Flee 逃跑
  - Hurt 受击
  - Dead 死亡

- Boss 三阶段切换
  - HP 阈值触发阶段转换
  - 狂暴模式属性提升

### 反作弊（三层检测）

Layer 1 实时行为检测
- 移速异常
- 攻击频率异常
- 闪避频率异常

Layer 2 逻辑校验
- 伤害数值校验
- 属性一致性校验
- 战斗状态合法性

Layer 3 回放分析
- 操作频率异常检测
- 时间戳重复检测
- 综合风险评分

响应等级：警告 → 回滚 → 踢出 → 封禁

### 帧同步

- 固定步长 20ms（50fps）
- 帧内执行顺序
  1. 玩家操作输入
  2. AI 决策更新
  3. Buff 效果结算
  4. 死亡资源回收
  5. 生成帧快照广播

- 客户端预测与回滚
  - 本地移动预测
  - 0.5m 偏差阈值
  - 服务器位置校正

## 八、测试

### 服务端测试

```bash
cd server
mvn test
```

- 总计 192 个单元测试
- 覆盖：战斗/AI/Buff/房间
- 覆盖：反作弊/审计/回放
- 覆盖：全链路集成
- 覆盖：并发与边界

### 客户端测试

Unity Test Framework
- 14 个 C# 协议测试
- 二进制序列化往返
- 消息包装器校验

## 九、开发文档

- [CODE_WIKI.md](CODE_WIKI.md)
  模块详细设计与接口说明

- [DOCKER_README.md](DOCKER_README.md)
  容器化部署与运维指南

- [KUBERNETES.md](docs/deployment/KUBERNETES.md)
  Kubernetes 部署、水平扩容边界与房间路由前置

- [AUDIT_REPORT_PHASE1_7.md](AUDIT_REPORT_PHASE1_7.md)
  代码审计报告与修复记录

## 十、后续扩展

按 JD 能力点分阶段补齐：

- P0：MySQL/Redis 持久化、压测工具与 benchmark 报告（已完成）
- P1：可观测性（指标/日志/告警）、CI/CD（已完成）
- P2：分布式部署、水平扩容、K8s（K8s 清单、NODE_ID 注册与房间所有权路由已完成，网关与房间迁移待补）
