# ActionGameDemo Docker 部署指南

## 快速启动

### 1. 构建并启动 (docker-compose)
```bash
# 复制环境变量配置
cp .env.example .env

# 构建并启动
docker-compose up -d --build

# 查看日志
docker-compose logs -f game-server

# 停止
docker-compose down
```

### 2. 直接用 Docker 构建
```bash
# 构建镜像
docker build -t actiongame-server .

# 运行容器
docker run -d \
  --name actiongame-server \
  -p 9090:9090 \
  -p 9091:9091 \
  -v actiongame-logs:/app/logs \
  -v actiongame-replays:/app/replays \
  actiongame-server

# 查看日志
docker logs -f actiongame-server
```

## 配置

### 环境变量
| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SERVER_PORT` | 9090 | WebSocket 服务端口 |
| `METRICS_PORT` | 9091 | health/metrics/alerts 端口 |
| `NODE_ID` | local | 节点标识，用于日志与后续房间路由 |
| `JAVA_OPTS` | `-Xms256m -Xmx512m -XX:+UseG1GC` | JVM 参数 |

### 数据卷
| 路径 | 说明 |
|------|------|
| `/app/logs` | 服务端日志 (game-server.log, audit-*.jsonl) |
| `/app/replays` | 战斗回放文件 |

### 自定义端口
```bash
# docker-compose
SERVER_PORT=8080 docker-compose up -d

# docker run
docker run -d -p 8080:8080 -e SERVER_PORT=8080 actiongame-server
```

## 健康检查
容器内置 healthcheck，每 30 秒检测指标端口 `/health`：
```bash
docker inspect --format='{{.State.Health.Status}}' actiongame-server
```

## Kubernetes

K8s 清单与扩容边界说明见 [KUBERNETES.md](docs/deployment/KUBERNETES.md)。

## 镜像信息
- **基础镜像**: `eclipse-temurin:17-jre-alpine` (轻量级 JRE)
- **构建镜像**: `maven:3.9.6-eclipse-temurin-17` (多阶段构建)
- **最终镜像大小**: ~200MB (不含 Maven 缓存)
- **JVM**: Eclipse Temurin 17 (LTS)
