# ActionGameDemo Observability

## 指标接口

| 接口 | 内容 |
|---|---|
| `/health` | 服务健康状态与当前连接数 |
| `/metrics` | Prometheus 文本格式指标 |
| `/alerts` | 当前告警 JSON |

默认端口：9091，可通过 `METRICS_PORT` 覆盖。

## 核心指标

- `actiongame_connections_active`：当前 WebSocket 连接数
- `actiongame_rooms_active`：当前活跃房间数
- `actiongame_messages_received_total`：收到消息总数
- `actiongame_messages_sent_total`：发送消息总数
- `actiongame_errors_total`：服务端错误总数
- `actiongame_frames_total`：已执行战斗帧总数
- `actiongame_actions_total`：玩家操作总数
- `actiongame_logins_total{result="success"|"failure"}`：登录成功/失败数

## 告警阈值

- 错误数 > 10
- 活跃房间 > 100
- 当前连接数 > 1000
- 登录失败率 > 50%

## 抓取示例

```bash
curl http://localhost:9091/metrics
curl http://localhost:9091/health
curl http://localhost:9091/alerts
```
