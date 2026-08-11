# ActionGameDemo Kubernetes 部署指南

## 架构

- `game-server` StatefulSet 提供 WebSocket 战斗服务，默认 2 个副本，Pod 名称为 `game-server-0`、`game-server-1`。
- `game-server` Service 暴露 `9090/game`（WebSocket）与 `9091`（health/metrics/alerts）。
- `game-server-headless` headless Service 提供稳定的 per-pod DNS：`game-server-0.game-server-headless.actiongame.svc.cluster.local`。
- `HorizontalPodAutoscaler` 基于 CPU 使用率自动扩缩容。
- `PodDisruptionBudget` 保证维护时至少保留 1 个可用 Pod。
- `mysql` StatefulSet 与 `redis` Deployment 是本地集群演示用的基础设施，生产环境建议替换为托管数据库/缓存。

## 快速启动

```bash
docker build -t actiongame-server:dev .
kubectl apply -k deploy/k8s/overlays/dev
kubectl -n actiongame rollout status statefulset/game-server
kubectl -n actiongame get pods
```

本机验证 WebSocket：

```bash
kubectl -n actiongame port-forward svc/game-server 9090:9090
# ws://127.0.0.1:9090/game
```

验证指标：

```bash
kubectl -n actiongame port-forward svc/game-server 9091:9091
curl http://127.0.0.1:9091/health
curl http://127.0.0.1:9091/metrics
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SERVER_PORT` | `9090` | WebSocket 服务端口 |
| `METRICS_PORT` | `9091` | health/metrics/alerts 端口 |
| `NODE_ID` | Pod 名称 | 节点标识，用于节点注册与房间所有权 |
| `NODE_ADDRESS_TEMPLATE` | `${NODE_ID}.game-server-headless.actiongame.svc.cluster.local:9090` | per-pod 地址模板 |
| `NODE_PUBLIC_ADDRESS` | `localhost:9090` | 节点对外地址提示，由网关解析后用于重定向 |
| `AUTH_TOKEN` | 空 | 登录鉴权 token；留空表示本地验证模式不校验 |
| `MATCH_DB_URL` | `jdbc:mysql://mysql:3306/actiongame...` | 对局结果持久化 |
| `MATCH_DB_USER` | `root` | MySQL 用户 |
| `MATCH_DB_PASSWORD` | demo 值 | MySQL 密码 |
| `REDIS_HOST` | `redis` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `JAVA_OPTS` | JVM 参数 | 堆与 GC 配置 |

`MATCH_DB_*` 与 `REDIS_PASSWORD` 放在 `Secret` 中，`SERVER_PORT` 等放在 `ConfigMap` 中。当前 Secret 是本地 demo 凭据，外部环境必须替换。

## 镜像与 Overlay

```bash
# dev
kubectl apply -k deploy/k8s/overlays/dev

# prod
kubectl apply -k deploy/k8s/overlays/prod
```

把 `deploy/k8s/overlays/<env>/deployment-patch.yaml` 中的镜像名替换为实际 registry 地址。overlay 中覆盖了副本数、资源规格与 HPA 范围。

注意：`game-server` 已从 Deployment 改为 StatefulSet。如果旧环境已经存在同名 Deployment，需要先执行 `kubectl -n actiongame delete deployment/game-server`，再 apply 新清单。

## 外部入口

集群内使用 headless DNS；集群外默认通过 Ingress 暴露：

- `game-server-node-0/1/2`：固定选择对应 StatefulSet Pod 的 ClusterIP Service。
- Ingress host 规则：`game-server-0.example.com/game` 路由到 Pod `game-server-0`，以此类推。
- 把 `*.example.com` 的 DNS 解析到 Ingress Controller 地址。
- 开启 TLS 时，把 `NODE_ADDRESS_TEMPLATE` 改成 `${NODE_ID}.example.com:443`；未开启 TLS 时使用 `:80`。
- 目录接口 `/routing/room/{roomId}` 返回的就是该模板展开后的地址，Unity 收到 `-3` 后按这个地址重连。

## 探针与优雅退出

- startup/liveness 使用 `http://<pod>:9091/health`，readiness 使用 `/ready`。
- `POST /admin/drain` 将节点置为排空状态：不再接受新房间，已有房间可继续完成；`/ready` 在排空时返回 503。
- StatefulSet preStop 会先调用 `POST /admin/drain` 和 `POST /admin/migrate-all`，然后最多等待 60 秒，直到本节点活跃房间归零，再进入优雅退出流程。
- 容器 entrypoint 使用 `exec java ...`，K8s 发送 `SIGTERM` 时 Java shutdown hook 会关闭指标服务与 Netty 事件循环。
- StatefulSet 设置 `terminationGracePeriodSeconds: 30`，给滚动更新留出断线重连窗口。

## 水平扩容边界

当前 `BattleRoom` 仍是进程内状态，Redis 只保存房间元数据和 `roomId -> nodeId` 所有权。已实现的基础：

1. 服务启动时用 `NODE_ID` 向节点注册表登记，并通过心跳续约，节点停止时注销。
2. `RoomManager` 创建房间前原子抢占 `roomId -> nodeId`，避免两个 Pod 同时创建同一个房间。
3. `JoinRoomHandler` 发现房间属于其他节点时返回 `code=-3`，响应尾部附带 owner node 与地址，旧客户端忽略新增字段。
4. `9091` 管理端口提供 `GET /routing/room/{roomId}`，返回 owner node、地址与分配状态。
5. Unity 验证客户端收到 `-3` 后自动断开旧节点，并使用返回地址重新连接同一房间。
6. 空房间 ID 由 `GlobalMatchmaker` 选择注册表中房间数最少的节点，并在 Redis 中预占 `roomId -> nodeId`。
7. 节点缩容时可调用 `POST /admin/migrate-all`，将玩家/怪物/Buff 快照写入 Redis、释放所有权，其他节点 Join 时自动恢复。
8. `BattleRoom` 每 100 帧自动写入迁移快照；owner 节点失活时，Join 请求会释放陈旧所有权并从快照恢复。

因此 K8s 多副本可以按房间分配到不同 Pod。当前迁移是“快照 + 客户端重连”，正式对战斗 Pod 开启自动缩容前，还需要补：

1. 提供 Ingress/NodePort/LoadBalancer 作为集群外入口，让 headless DNS 地址能被外部客户端连接。
2. 完整在线迁移：Buff 状态、回放连续性、客户端无感切换，避免缩容打断正在进行的战斗。

在外部入口与完整在线迁移完成前，建议固定战斗 Pod 数量，用 `kubectl scale` 只做节点级扩容，而不是依赖 HPA 缩容。

## 数据目录

- `/app/logs` 使用 `emptyDir`，日志同时输出到 stdout，由容器运行时收集。
- `/app/replays` 使用 `emptyDir`，回放文件在 Pod 重建后会丢失；生产环境应改为对象存储或挂载可共享存储。
