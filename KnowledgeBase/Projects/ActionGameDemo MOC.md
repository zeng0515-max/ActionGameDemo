---
title: ActionGameDemo MOC
date: 2026-07-30
tags: [project, moc, unity, java, netty, websocket, game]
source: 历史记录蒸馏
status: Doing
---

# ActionGameDemo MOC

> [!info] 项目定位
> Realtime Combat Server：以实时对战服务端为核心产品的完整后端项目，Unity 客户端仅作为验证 Demo。

## 核心特征

- 客户端-服务器架构，WebSocket 二进制协议
- 服务端权威判定，客户端纯表现
- 50fps 帧快照状态同步
- 三层反作弊检测（实时 / 逻辑 / 回放）
- AI 敌人 FSM + Boss AI
- 对象池驱动 VFX 特效系统

## 技术栈

| 层 | 技术 |
|---|---|
| 客户端 | Unity 2022 LTS, C# |
| 服务端 | Java 17, Netty 4.1.108 |
| 协议 | WebSocket + Protobuf |
| 部署 | Docker, docker-compose |
| 测试 | JUnit 5, Mockito, AssertJ |

## 知识索引

### 项目文档
- [[ActionGameDemo 架构分析]] — 模块划分与调用链
- [[ActionGameDemo 代码改进清单]] — 24 条代码级改进点
- [[CODE_WIKI]] — 完整项目百科（21 章节）

### 求职相关
- [[求职准备行动清单]] — 大厂 AI Agent 实习全流程
- [[ActionGameDemo 修复任务]] — P0/P1/P2 修复优先级

### 知识卡片
- [[服务端权威架构]] — 为什么客户端不做判定
- [[状态同步 vs 帧同步]] — 概念辨析与选型
- [[三层反作弊设计]] — 实时/逻辑/回放分层
- [[伤害管线模式]] — Pipeline 可插拔设计
- [[对象池优化实践]] — VFX 内存管理

## 秋招能力映射

| JD 能力点 | 项目模块 |
|---|---|
| 网络编程/高并发 | WebSocket、Netty Pipeline、帧调度 |
| 服务端架构设计 | Room、Battle、Replay、AntiCheat |
| 稳定性与安全 | 三层反作弊、审计、断线重连 |
| 性能优化 | 压测工具与 benchmark 报告（已完成） |
| 数据库/中间件 | MySQL/Redis 持久化（已接入，可降级内存） |
| 监控与运维 | 指标/日志/告警、CI/CD（已完成） |
| 工程化 | JUnit、Docker、文档 |
| AI 加分 | LLM 复盘与异常研判 |

## 关键文件速查

| 文件 | 职责 |
|---|---|
| `Assets/Scripts/Core/GameManager.cs` | 全局状态机 |
| `Assets/Scripts/Client/Network/NetworkClient.cs` | 网络客户端主类 |
| `Assets/Scripts/Client/Network/FrameSnapshotManager.cs` | 帧快照管理 |
| `Assets/Scripts/Combat/VFXEffectManager.cs` | 特效对象池 |
| `server/.../room/BattleRoom.java` | 战斗房间核心 |
| `server/.../battle/combatsystem/CombatSystem.java` | 战斗系统 |
| `server/.../anticheat/CheatDetector.java` | 反作弊检测 |
| `server/.../room/FrameScheduler.java` | 帧调度器 |

## 外部链接

- [[CODE_WIKI]] — 项目根目录 `CODE_WIKI.md`
- GitHub 仓库（如已推送）
