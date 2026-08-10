---
title: ActionGameDemo 项目总览
date: 2026-08-04
tags:
  - "#actiongame"
  - "#project"
  - "#unreal"
  - "#multiplayer"
source: 内部研发项目
status: 立项启动
---

# ActionGameDemo 项目总览

> [!warning] 文档定位
> 本文档是远期产品规划，不代表当前仓库实现。
> 当前仓库实际实现：Unity 2022.3 客户端 + Java/Netty 权威服务端，项目定位见 README.md。

## 项目简介
Realtime Combat Server：以实时对战服务端为核心产品
Unity 客户端作为验证 Demo
聚焦权威战斗、帧同步、反作弊与可观测性

## 当前实现 vs 远期规划

| 维度 | 当前实现 | 远期规划 |
|---|---|---|
| 客户端 | Unity 2022.3 + C# | Unreal Engine 5.3 |
| 服务端 | Java 17 + Netty + WebSocket | Go 微服务 + gRPC |
| 数据 | 暂未持久化 | Redis + PostgreSQL |
| 定位 | 实时对战服务端框架 | 完整多人游戏产品 |

## 十三阶段里程碑时间线

**阶段01：项目立项**
2026-08 完成立项评审
确认目标与核心玩法

**阶段02：原型搭建**
2026-09 基础角色控制
完成移动与基础动作

**阶段03：战斗雏形**
2026-10 近战与远程攻击
实现命中判定基础

**阶段04：网络同步**
2026-11 帧同步/状态同步
完成基础联机对战

**阶段05：AI 对手**
2026-12 基础行为树
实现巡逻与追击逻辑

**阶段06：关卡原型**
2027-01 基础地图搭建
完成出生点与掩体

**阶段07：技能系统**
2027-02 技能树与冷却
实现 Buff/Debuff

**阶段08：UI 系统**
2027-03 HUD 与菜单
背包与技能栏界面

**阶段09：反作弊**
2027-04 服务端校验
客户端完整性检查

**阶段10：性能优化**
2027-05 帧率与内存优化
网络带宽调优

**阶段11：音效与特效**
2027-06 技能特效接入
背景音效与配音

**阶段12：平衡性调优**
2027-07 数值调整
多轮内测反馈迭代

**阶段13：发布准备**
2027-08 打包与发布文档
完成交付验收

## 架构红线

**同步红线**
关键状态服务端权威
禁止客户端逻辑改数值

**性能红线**
单帧 CPU < 16ms
单客户端带宽 < 64kb/s

**安全红线**
内存哈希实时校验
关键操作二次签名

**代码红线**
核心模块单测覆盖 > 80%
禁止硬编码配置项

## 核心模块索引

- [[战斗系统/Combat_Core]] 战斗核心
- [[战斗系统/Damage_Calculator]] 伤害计算
- [[战斗系统/Skill_System]] 技能系统
- [[网络同步/Network_Framework]] 网络框架
- [[网络同步/State_Sync]] 状态同步
- [[AI系统/Behavior_Tree]] 行为树
- [[AI系统/Pathfinding]] 寻路系统
- [[反作弊审计/AntiCheat_Core]] 反作弊核心
- [[反作弊审计/Client_Verify]] 客户端校验
- [[工程规范/Code_Style]] 代码规范
- [[工程规范/Build_Pipeline]] 构建流程

## 技术栈清单

**引擎**
Unreal Engine 5.3
C++ 与蓝图混合

**网络**
UE Replication 系统
自定义帧同步层

**数据库**
Redis 会话缓存
PostgreSQL 持久化

**后端服务**
Go 微服务架构
gRPC 内部通信

**运维监控**
Prometheus 指标
Grafana 可视化

## 代码仓库路径

**主客户端仓库**
`d:\ActionGameDemo\Client`

**主服务端仓库**
`d:\ActionGameDemo\Server`

**运维与工具仓库**
`d:\ActionGameDemo\Ops`

**美术资源仓库**
`d:\ActionGameDemo\Assets`
