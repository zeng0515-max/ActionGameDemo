# ActionGameDemo 后端化改造 Phase 1 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 ActionGameDemo 从“Unity 动作游戏原型”重构定位为“Realtime Combat Server + Unity 验证客户端”，并同步更新 README、CODE_WIKI、KnowledgeBase、Vault 文档。

**Architecture:** 文档先行，不改服务端与客户端代码。以设计 spec 为唯一事实源，把新定位、模块边界、JD 能力映射写入现有文档；MySQL/Redis、压测、可观测性等代码补齐单独成后续 Phase 计划。

**Tech Stack:** Markdown、Obsidian Vault、Git。

---

## 范围说明

Phase 1 只做项目定义与文档重构。后续 Phase 2 做数据层（MySQL/Redis），Phase 3 做压测与 benchmark，Phase 4 做可观测性与 CI/CD，Phase 5 做分布式与 K8s。每个 Phase 单独一份计划。

## 文件结构

- Modify: `README.md`
- Modify: `CODE_WIKI.md`
- Modify: `KnowledgeBase/Projects/ActionGameDemo MOC.md`
- Modify: `Vault/Projects/ActionGameDemo.md`
- Modify: `Vault/Tasks/ActionGameDemo_秋招准备清单.md`

## Task 1: 重写 README 项目定位

**Files:**
- Modify: `README.md:1-19`
- Modify: `README.md:13-24`
- Modify: `README.md` 末尾 `## 十、后续扩展` 小节

- [ ] **Step 1: 替换 README 顶部定位**

将文件开头：

```markdown
# ActionGameDemo

Unity 动作游戏原型
采用客户端-服务器架构
Java 权威战斗服务端
Unity 纯表现客户端

## 一、项目概述

ActionGameDemo 是一款
第三人称动作游戏 Demo
聚焦游戏服务端开发
展示完整 C/S 架构方案
```

替换为：

```markdown
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
```

- [ ] **Step 2: 替换核心特性列表**

将 `### 核心特性` 下的旧列表替换为：

```markdown
### 核心特性

- 50fps 帧同步状态广播
- WebSocket 二进制协议与 Netty Pipeline
- 服务端权威战斗计算
- 房间管理、帧调度与断线重连
- 三层实时反作弊检测
- AI 敌人 FSM 行为系统
- Boss 多阶段狂暴切换
- 战斗录像回放与结构化审计
- LLM 战斗复盘分析
- 145 个单元测试覆盖
- Docker 一键部署
```

- [ ] **Step 3: 替换后续扩展为分阶段路线**

将 `## 十、后续扩展` 小节整体替换为：

```markdown
## 十、后续扩展

按 JD 能力点分阶段补齐：

- P0：MySQL/Redis 持久化、压测工具与 benchmark 报告
- P1：可观测性（指标/日志/告警）、CI/CD
- P2：分布式部署、水平扩容、K8s
```

- [ ] **Step 4: 验证旧定位已清除**

Run: `rg -n "Unity 动作游戏原型|第三人称动作游戏 Demo" README.md`

Expected: 无匹配输出。

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: reposition project as realtime combat server"
```

## Task 2: 更新 KnowledgeBase MOC

**Files:**
- Modify: `KnowledgeBase/Projects/ActionGameDemo MOC.md:11-12`
- Modify: `KnowledgeBase/Projects/ActionGameDemo MOC.md`，在 `## 关键文件速查` 前新增章节

- [ ] **Step 1: 替换项目定位引用**

将：

```markdown
> [!info] 项目定位
> Unity 动作游戏原型 + Java 服务端权威架构，用于证明工程能力。
```

替换为：

```markdown
> [!info] 项目定位
> Realtime Combat Server：以实时对战服务端为核心产品的完整后端项目，Unity 客户端仅作为验证 Demo。
```

- [ ] **Step 2: 新增秋招能力映射章节**

在 `## 关键文件速查` 之前插入：

```markdown
## 秋招能力映射

| JD 能力点 | 项目模块 |
|---|---|
| 网络编程/高并发 | WebSocket、Netty Pipeline、帧调度 |
| 服务端架构设计 | Room、Battle、Replay、AntiCheat |
| 稳定性与安全 | 三层反作弊、审计、断线重连 |
| 性能优化 | 压测与 benchmark（P0） |
| 数据库/中间件 | MySQL/Redis（P0） |
| 监控与运维 | 指标/日志/告警（P1） |
| 工程化 | JUnit、Docker、文档 |
| AI 加分 | LLM 复盘与异常研判 |
```

- [ ] **Step 3: 验证**

Run: `rg -n "Unity 动作游戏原型" "KnowledgeBase/Projects/ActionGameDemo MOC.md"`

Expected: 无匹配输出。

- [ ] **Step 4: Commit**

```bash
git add "KnowledgeBase/Projects/ActionGameDemo MOC.md"
git commit -m "docs: align MOC with backend positioning"
```

## Task 3: 修正 Vault 项目总览

**Files:**
- Modify: `Vault/Projects/ActionGameDemo.md:11-17`
- Modify: `Vault/Projects/ActionGameDemo.md`，在 `## 十三阶段里程碑时间线` 前新增章节

- [ ] **Step 1: 添加文档定位警告**

在 `# ActionGameDemo 项目总览` 标题后插入：

```markdown
> [!warning] 文档定位
> 本文档是远期产品规划，不代表当前仓库实现。
> 当前仓库实际实现：Unity 2022.3 客户端 + Java/Netty 权威服务端，项目定位见 README.md。
```

- [ ] **Step 2: 替换项目简介**

将：

```markdown
## 项目简介
第三人称动作游戏原型
聚焦多人联机对抗玩法
验证战斗与同步核心技术
```

替换为：

```markdown
## 项目简介
Realtime Combat Server：以实时对战服务端为核心产品
Unity 客户端作为验证 Demo
聚焦权威战斗、帧同步、反作弊与可观测性
```

- [ ] **Step 3: 新增当前实现 vs 远期规划表**

在 `## 十三阶段里程碑时间线` 之前插入：

```markdown
## 当前实现 vs 远期规划

| 维度 | 当前实现 | 远期规划 |
|---|---|---|
| 客户端 | Unity 2022.3 + C# | Unreal Engine 5.3 |
| 服务端 | Java 17 + Netty + WebSocket | Go 微服务 + gRPC |
| 数据 | 暂未持久化 | Redis + PostgreSQL |
| 定位 | 实时对战服务端框架 | 完整多人游戏产品 |
```

- [ ] **Step 4: Commit**

```bash
git add "Vault/Projects/ActionGameDemo.md"
git commit -m "docs: clarify current implementation vs future plan"
```

## Task 4: 秋招清单增加 JD 能力映射

**Files:**
- Modify: `Vault/Tasks/ActionGameDemo_秋招准备清单.md`，在 `# 秋招面试准备清单` 后新增章节

- [ ] **Step 1: 插入 JD 能力对齐表**

在标题后插入：

```markdown
## 〇、JD 能力对齐

| 大厂 JD 能力点 | 项目对应模块 | 可讲问题 |
|---|---|---|
| 高并发与网络编程 | Netty、WebSocket 二进制协议、帧调度 | 50fps 快照如何控制带宽 |
| 服务端架构设计 | Room/Battle/Replay/AntiCheat 分层 | 模块边界如何划分 |
| 稳定性与安全 | 三层反作弊、审计、断线重连 | 反作弊如何减少误判 |
| 数据库/中间件 | MySQL/Redis（P0） | 对局数据如何持久化 |
| 性能优化 | 压测与 benchmark（P0） | 单帧 CPU 与带宽指标 |
| 监控与运维 | 指标/日志/告警（P1） | 线上问题如何定位 |
| AI 应用 | LLM 复盘与异常研判 | 如何用 LLM 做局后分析 |
```

- [ ] **Step 2: Commit**

```bash
git add "Vault/Tasks/ActionGameDemo_秋招准备清单.md"
git commit -m "docs: add JD capability mapping to interview checklist"
```

## Task 5: 更新 CODE_WIKI 项目概述

**Files:**
- Modify: `CODE_WIKI.md:1-19`
- Modify: `CODE_WIKI.md`，在 `### 2.2 架构红线` 后新增小节

- [ ] **Step 1: 替换项目概述**

将文件开头：

```markdown
# ActionGameDemo 项目百科

## 一、项目概述

ActionGameDemo 是一款
Unity 动作游戏原型
采用客户端-服务器架构

客户端负责表现与输入
服务端负责权威判定
通信基于 WebSocket 二进制协议

### 核心特性

- 帧同步状态广播（50fps）
- 服务端权威战斗计算
- 实时反作弊三层检测
- AI 敌人行为树与 FSM
- 对象池驱动的视觉特效
```

替换为：

```markdown
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
```

- [ ] **Step 2: 新增后端能力对齐小节**

在 `### 2.2 架构红线` 末尾之后插入：

```markdown
### 2.3 后端能力对齐

| JD 能力点 | CODE_WIKI 对应章节 |
|---|---|
| 网络编程/高并发 | 四.2 Net 网络层、五.2 帧同步机制 |
| 服务端架构设计 | 四.3 Room、四.4 Battle |
| 稳定性与安全 | 四.9 AntiCheat、四.12 Audit |
| 数据库/中间件 | P0 待补齐 |
| 性能优化 | 十六 关键性能考量 |
| AI 应用 | 四.10 LLM 服务 |
```

- [ ] **Step 3: 验证旧定位已清除**

Run: `rg -n "Unity 动作游戏原型" CODE_WIKI.md`

Expected: 无匹配输出。

- [ ] **Step 4: Commit**

```bash
git add CODE_WIKI.md
git commit -m "docs: align code wiki with backend positioning"
```

## Task 6: 全局一致性验证

**Files:**
- 无新增，只读验证

- [ ] **Step 1: 全仓检查旧定位文案**

Run: `rg -n "Unity 动作游戏原型|第三人称动作游戏 Demo" README.md CODE_WIKI.md "KnowledgeBase/Projects/ActionGameDemo MOC.md" "Vault/Projects/ActionGameDemo.md" "Vault/Tasks/ActionGameDemo_秋招准备清单.md"`

Expected: 无匹配输出。

- [ ] **Step 2: 确认只改了文档**

Run: `git status --short -- README.md CODE_WIKI.md "KnowledgeBase/Projects/ActionGameDemo MOC.md" "Vault/Projects/ActionGameDemo.md" "Vault/Tasks/ActionGameDemo_秋招准备清单.md"`

Expected: 只有这 5 个文档文件出现在输出中；本次执行不触碰 `server/` 与 `Assets/`。

- [ ] **Step 3: 汇总提交**

如果存在未提交的文档改动：

```bash
git add README.md CODE_WIKI.md "KnowledgeBase/Projects/ActionGameDemo MOC.md" "Vault/Projects/ActionGameDemo.md" "Vault/Tasks/ActionGameDemo_秋招准备清单.md"
git commit -m "docs: complete backend repositioning phase 1"
```

否则无需额外提交。
