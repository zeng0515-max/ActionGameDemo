# ActionGameDemo 后端化改造 Phase 4 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Realtime Combat Server 补齐可观测性与 CI：统一指标采集、健康检查、告警评估、Prometheus 指标出口、Grafana Dashboard 定义，以及 GitHub Actions 自动构建测试流水线。

**Architecture:** 在 Netty WebSocket Pipeline 中插入 `MetricsChannelHandler`，统一统计收/发消息与连接数；`MetricsHttpServer` 基于 JDK `HttpServer` 暴露 `/health`、`/metrics`、`/alerts` 三个 HTTP 接口；`AlertEvaluator` 根据 `ServerMetrics` 阈值生成告警；CI 使用 GitHub Actions 在 Java 17 + Maven 环境下执行全量测试。

**Tech Stack:** Java 17、Netty、JDK HttpServer、Prometheus 文本格式、GitHub Actions、Markdown。

---

## 范围说明

Phase 4 只做服务端可观测性与 CI，不修改 Unity 客户端，不新增美术/建模内容，不改 MySQL/Redis 持久化行为。

## 文件结构

- Create: `server/src/main/java/com/actiongame/server/observability/MetricsChannelHandler.java`
- Create: `server/src/main/java/com/actiongame/server/observability/AlertEvaluator.java`
- Create: `server/src/main/java/com/actiongame/server/observability/MetricsHttpServer.java`
- Modify: `server/src/main/java/com/actiongame/server/net/WebSocketServer.java`
- Modify: `server/src/main/java/com/actiongame/server/net/WebSocketServerHandler.java`
- Modify: `server/src/main/java/com/actiongame/server/Bootstrap.java`
- Create: `.github/workflows/ci.yml`
- Create: `docs/observability/OBSERVABILITY.md`
- Create: `docs/observability/grafana-dashboard.json`
- Create: `docs/ci-cd/CI_CD.md`
- Test: `server/src/test/java/com/actiongame/server/observability/MetricsChannelHandlerTest.java`
- Test: `server/src/test/java/com/actiongame/server/observability/AlertEvaluatorTest.java`
- Test: `server/src/test/java/com/actiongame/server/observability/MetricsHttpServerTest.java`

## Task 1: Netty 指标采集

**Files:**
- Create: `server/src/main/java/com/actiongame/server/observability/MetricsChannelHandler.java`
- Modify: `server/src/main/java/com/actiongame/server/net/WebSocketServer.java`
- Modify: `server/src/main/java/com/actiongame/server/net/WebSocketServerHandler.java`
- Test: `server/src/test/java/com/actiongame/server/observability/MetricsChannelHandlerTest.java`

- [ ] **Step 1: 先写失败测试**

创建 `server/src/test/java/com/actiongame/server/observability/MetricsChannelHandlerTest.java`：

```java
package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import com.actiongame.server.net.util.MessageHelper;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Netty 指标采集")
class MetricsChannelHandlerTest {

    @Test
    @DisplayName("should_countInboundAndOutboundMessages")
    void should_countInboundAndOutboundMessages() {
        ServerMetrics metrics = new ServerMetrics();
        MetricsChannelHandler handler = new MetricsChannelHandler(metrics);
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        channel.writeInbound(MessageHelper.wrap(MessageId.HEARTBEAT_REQ, 1, new byte[0]));
        channel.writeOutbound(MessageHelper.wrap(MessageId.HEARTBEAT_RESP, 1, new byte[0]));

        assertThat(metrics.getTotalMessagesReceived()).isEqualTo(1);
        assertThat(metrics.getTotalMessagesSent()).isEqualTo(1);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -f server/pom.xml test -Dtest=MetricsChannelHandlerTest`

Expected: FAIL，原因是 `MetricsChannelHandler` 不存在。

- [ ] **Step 3: 实现 MetricsChannelHandler**

创建 `server/src/main/java/com/actiongame/server/observability/MetricsChannelHandler.java`：

```java
package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class MetricsChannelHandler extends ChannelDuplexHandler {
    private final ServerMetrics metrics;

    public MetricsChannelHandler(ServerMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof MessageWrapper) {
            metrics.onMessageReceived();
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof MessageWrapper) {
            metrics.onMessageSent();
        }
        ctx.write(msg, promise);
    }
}
```

- [ ] **Step 4: WebSocketServer 接入指标**

在 `WebSocketServer` 新增字段：

```java
    private final com.actiongame.server.audit.ServerMetrics metrics;
```

将构造器：

```java
    public WebSocketServer(int port, ConnectionManager connectionManager, HandlerRegistry handlerRegistry) {
        this.port = port;
        this.connectionManager = connectionManager;
        this.handlerRegistry = handlerRegistry;
    }
```

替换为：

```java
    public WebSocketServer(int port, ConnectionManager connectionManager, HandlerRegistry handlerRegistry) {
        this(port, connectionManager, handlerRegistry, new com.actiongame.server.audit.ServerMetrics());
    }

    public WebSocketServer(int port, ConnectionManager connectionManager, HandlerRegistry handlerRegistry,
                           com.actiongame.server.audit.ServerMetrics metrics) {
        this.port = port;
        this.connectionManager = connectionManager;
        this.handlerRegistry = handlerRegistry;
        this.metrics = metrics;
    }
```

在 Pipeline 中 `encoder` 之后新增：

```java
                        pipeline.addLast("metrics", new MetricsChannelHandler(metrics));
```

并将：

```java
                        pipeline.addLast("serverHandler", new WebSocketServerHandler(connectionManager, handlerRegistry));
```

替换为：

```java
                        pipeline.addLast("serverHandler", new WebSocketServerHandler(connectionManager, handlerRegistry, metrics));
```

在文件顶部新增 import：

```java
import com.actiongame.server.observability.MetricsChannelHandler;
```

- [ ] **Step 5: WebSocketServerHandler 接入连接与错误指标**

在 `WebSocketServerHandler` 新增字段：

```java
    private final com.actiongame.server.audit.ServerMetrics metrics;
```

新增构造器重载：

```java
    public WebSocketServerHandler(ConnectionManager connectionManager, HandlerRegistry handlerRegistry) {
        this(connectionManager, handlerRegistry, null);
    }

    public WebSocketServerHandler(ConnectionManager connectionManager, HandlerRegistry handlerRegistry,
                                  com.actiongame.server.audit.ServerMetrics metrics) {
        this.connectionManager = connectionManager;
        this.handlerRegistry = handlerRegistry;
        this.metrics = metrics;
    }
```

在 `channelActive` 中 `createSession` 后新增：

```java
        if (metrics != null) metrics.onConnect();
```

在 `channelInactive` 中 `removeSession` 后新增：

```java
        if (metrics != null) metrics.onDisconnect();
```

在 `exceptionCaught` 中 `ctx.close()` 前新增：

```java
        if (metrics != null) metrics.onError("WebSocketServerHandler");
```

- [ ] **Step 6: 运行测试与编译**

Run: `mvn -q -f server/pom.xml test -Dtest=MetricsChannelHandlerTest`

Expected: PASS。

Run: `mvn -q -f server/pom.xml compile`

Expected: BUILD SUCCESS。

- [ ] **Step 7: 自查**

检查点：
- 指标统计的是真实 WebSocket 消息收发，不是 Mock 数据。
- 原有 `new WebSocketServer(...)` 调用仍兼容，默认创建独立 `ServerMetrics`。
- 没有改协议、没有改 Unity 客户端。

- [ ] **Step 8: Commit**

```bash
git add server/src/main/java/com/actiongame/server/observability server/src/main/java/com/actiongame/server/net/WebSocketServer.java server/src/main/java/com/actiongame/server/net/WebSocketServerHandler.java server/src/test/java/com/actiongame/server/observability
git commit -m "feat: collect netty message and connection metrics"
```

## Task 2: 告警规则

**Files:**
- Create: `server/src/main/java/com/actiongame/server/observability/AlertEvaluator.java`
- Test: `server/src/test/java/com/actiongame/server/observability/AlertEvaluatorTest.java`

- [ ] **Step 1: 先写失败测试**

创建 `server/src/test/java/com/actiongame/server/observability/AlertEvaluatorTest.java`：

```java
package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("告警规则")
class AlertEvaluatorTest {

    @Test
    @DisplayName("should_triggerAlerts_when_thresholdExceeded")
    void should_triggerAlerts_when_thresholdExceeded() {
        ServerMetrics metrics = new ServerMetrics();
        for (int i = 0; i < 11; i++) metrics.onError("test");
        for (int i = 0; i < 101; i++) metrics.onBattleStart();
        AlertEvaluator evaluator = new AlertEvaluator(10, 100, 1000);

        List<String> alerts = evaluator.evaluate(metrics);

        assertThat(alerts).anyMatch(a -> a.contains("error"));
        assertThat(alerts).anyMatch(a -> a.contains("rooms"));
    }

    @Test
    @DisplayName("should_triggerLoginFailureAlert_when_rateHigh")
    void should_triggerLoginFailureAlert_when_rateHigh() {
        ServerMetrics metrics = new ServerMetrics();
        metrics.onLoginFailure();
        metrics.onLoginFailure();
        metrics.onLoginSuccess();
        AlertEvaluator evaluator = new AlertEvaluator();

        List<String> alerts = evaluator.evaluate(metrics);

        assertThat(alerts).anyMatch(a -> a.contains("login"));
    }

    @Test
    @DisplayName("should_returnEmpty_when_healthy")
    void should_returnEmpty_when_healthy() {
        AlertEvaluator evaluator = new AlertEvaluator();

        assertThat(evaluator.evaluate(new ServerMetrics())).isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -f server/pom.xml test -Dtest=AlertEvaluatorTest`

Expected: FAIL，原因是 `AlertEvaluator` 不存在。

- [ ] **Step 3: 实现 AlertEvaluator**

创建 `server/src/main/java/com/actiongame/server/observability/AlertEvaluator.java`：

```java
package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;

import java.util.ArrayList;
import java.util.List;

public class AlertEvaluator {
    private final long maxErrors;
    private final long maxActiveRooms;
    private final long maxConnections;

    public AlertEvaluator() {
        this(10, 100, 1000);
    }

    public AlertEvaluator(long maxErrors, long maxActiveRooms, long maxConnections) {
        this.maxErrors = maxErrors;
        this.maxActiveRooms = maxActiveRooms;
        this.maxConnections = maxConnections;
    }

    public List<String> evaluate(ServerMetrics metrics) {
        List<String> alerts = new ArrayList<>();
        if (metrics.getTotalErrors() > maxErrors) {
            alerts.add("High error count: " + metrics.getTotalErrors() + " > " + maxErrors);
        }
        if (metrics.getActiveRooms() > maxActiveRooms) {
            alerts.add("High active rooms: " + metrics.getActiveRooms() + " > " + maxActiveRooms);
        }
        if (metrics.getCurrentConnections() > maxConnections) {
            alerts.add("High connections: " + metrics.getCurrentConnections() + " > " + maxConnections);
        }
        long totalLogins = metrics.getLoginSuccess() + metrics.getLoginFailures();
        if (totalLogins > 0 && metrics.getLoginFailures() * 100 / totalLogins > 50) {
            alerts.add("High login failure rate: " + metrics.getLoginFailures() + "/" + totalLogins);
        }
        return alerts;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -f server/pom.xml test -Dtest=AlertEvaluatorTest`

Expected: PASS，3 个测试全部通过。

- [ ] **Step 5: 自查**

检查点：
- 阈值可通过构造器配置，测试可控。
- 告警只读指标，不修改状态。
- 登录失败率用整数百分比，不会除零。

- [ ] **Step 6: Commit**

```bash
git add server/src/main/java/com/actiongame/server/observability server/src/test/java/com/actiongame/server/observability
git commit -m "feat: add alert evaluation rules"
```

## Task 3: 指标 HTTP 服务

**Files:**
- Create: `server/src/main/java/com/actiongame/server/observability/MetricsHttpServer.java`
- Test: `server/src/test/java/com/actiongame/server/observability/MetricsHttpServerTest.java`

- [ ] **Step 1: 先写失败测试**

创建 `server/src/test/java/com/actiongame/server/observability/MetricsHttpServerTest.java`：

```java
package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("指标 HTTP 服务")
class MetricsHttpServerTest {

    @Test
    @DisplayName("should_exposeHealthMetricsAndAlerts")
    void should_exposeHealthMetricsAndAlerts() throws Exception {
        ServerMetrics metrics = new ServerMetrics();
        metrics.onConnect();
        metrics.onMessageReceived();
        MetricsHttpServer server = new MetricsHttpServer(0, metrics, new AlertEvaluator());
        server.start();
        try {
            String health = get(server.getPort(), "/health");
            assertThat(health).contains("\"status\":\"ok\"");

            String prometheus = get(server.getPort(), "/metrics");
            assertThat(prometheus).contains("actiongame_connections_active");
            assertThat(prometheus).contains("actiongame_messages_received_total");

            String alerts = get(server.getPort(), "/alerts");
            assertThat(alerts).contains("\"alerts\"");
        } finally {
            server.stop();
        }
    }

    private String get(int port, String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(
            "http://127.0.0.1:" + port + path).toURL().openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().reduce("", (a, b) -> a + b);
        } finally {
            connection.disconnect();
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -f server/pom.xml test -Dtest=MetricsHttpServerTest`

Expected: FAIL，原因是 `MetricsHttpServer` 不存在。

- [ ] **Step 3: 实现 MetricsHttpServer**

创建 `server/src/main/java/com/actiongame/server/observability/MetricsHttpServer.java`：

```java
package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class MetricsHttpServer {
    private static final Gson GSON = new Gson();

    private final HttpServer server;
    private final ServerMetrics metrics;
    private final AlertEvaluator alertEvaluator;

    public MetricsHttpServer(int port, ServerMetrics metrics, AlertEvaluator alertEvaluator) throws IOException {
        this.metrics = metrics;
        this.alertEvaluator = alertEvaluator;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext("/metrics", this::handleMetrics);
        server.createContext("/alerts", this::handleAlerts);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        String body = "{\"status\":\"ok\",\"activeConnections\":" + metrics.getCurrentConnections() + "}";
        writeJson(exchange, body);
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        writeText(exchange, prometheusText());
    }

    private void handleAlerts(HttpExchange exchange) throws IOException {
        writeJson(exchange, GSON.toJson(java.util.Map.of("alerts", alertEvaluator.evaluate(metrics))));
    }

    private String prometheusText() {
        return "# HELP actiongame_connections_active Active WebSocket connections\n"
            + "# TYPE actiongame_connections_active gauge\n"
            + "actiongame_connections_active " + metrics.getCurrentConnections() + "\n"
            + "# HELP actiongame_rooms_active Active battle rooms\n"
            + "# TYPE actiongame_rooms_active gauge\n"
            + "actiongame_rooms_active " + metrics.getActiveRooms() + "\n"
            + "# HELP actiongame_messages_received_total Received messages\n"
            + "# TYPE actiongame_messages_received_total counter\n"
            + "actiongame_messages_received_total " + metrics.getTotalMessagesReceived() + "\n"
            + "# HELP actiongame_messages_sent_total Sent messages\n"
            + "# TYPE actiongame_messages_sent_total counter\n"
            + "actiongame_messages_sent_total " + metrics.getTotalMessagesSent() + "\n"
            + "# HELP actiongame_errors_total Server errors\n"
            + "# TYPE actiongame_errors_total counter\n"
            + "actiongame_errors_total " + metrics.getTotalErrors() + "\n"
            + "# HELP actiongame_frames_total Executed battle frames\n"
            + "# TYPE actiongame_frames_total counter\n"
            + "actiongame_frames_total " + metrics.getTotalFramesExecuted() + "\n"
            + "# HELP actiongame_actions_total Player actions\n"
            + "# TYPE actiongame_actions_total counter\n"
            + "actiongame_actions_total " + metrics.getTotalPlayerActions() + "\n"
            + "# HELP actiongame_logins_total Login attempts\n"
            + "# TYPE actiongame_logins_total counter\n"
            + "actiongame_logins_total{result=\"success\"} " + metrics.getLoginSuccess() + "\n"
            + "actiongame_logins_total{result=\"failure\"} " + metrics.getLoginFailures() + "\n"
            + "# HELP actiongame_cheats_total Cheat incidents\n"
            + "# TYPE actiongame_cheats_total counter\n"
            + "actiongame_cheats_total " + metrics.getCheatIncidents() + "\n";
    }

    private void writeJson(HttpExchange exchange, String body) throws IOException {
        write(exchange, "application/json", body);
    }

    private void writeText(HttpExchange exchange, String body) throws IOException {
        write(exchange, "text/plain; version=0.0.4", body);
    }

    private void write(HttpExchange exchange, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -f server/pom.xml test -Dtest=MetricsHttpServerTest`

Expected: PASS。

- [ ] **Step 5: 自查**

检查点：
- `/metrics` 是标准 Prometheus 文本格式，可直接被抓取。
- `/alerts` 返回 JSON，方便接入告警平台。
- 测试使用随机端口，不依赖固定端口。

- [ ] **Step 6: Commit**

```bash
git add server/src/main/java/com/actiongame/server/observability server/src/test/java/com/actiongame/server/observability
git commit -m "feat: add metrics http server with prometheus format"
```

## Task 4: Bootstrap 接入指标服务

**Files:**
- Modify: `server/src/main/java/com/actiongame/server/Bootstrap.java`

- [ ] **Step 1: 修改 Bootstrap**

在 `Bootstrap` 中新增常量：

```java
    private static final int DEFAULT_METRICS_PORT = 9091;
```

在 `main` 中读取指标端口：

```java
        int metricsPort = DEFAULT_METRICS_PORT;
        String metricsPortEnv = System.getenv("METRICS_PORT");
        if (metricsPortEnv != null && !metricsPortEnv.isBlank()) {
            try {
                metricsPort = Integer.parseInt(metricsPortEnv);
            } catch (NumberFormatException e) {
                log.warn("Invalid METRICS_PORT '{}', using default {}", metricsPortEnv, DEFAULT_METRICS_PORT);
            }
        }
```

将：

```java
        WebSocketServer server = new WebSocketServer(port, connectionManager, handlerRegistry);
```

替换为：

```java
        var metrics = auditLogger.getMetrics();
        WebSocketServer server = new WebSocketServer(port, connectionManager, handlerRegistry, metrics);
        com.actiongame.server.observability.MetricsHttpServer metricsServer =
            new com.actiongame.server.observability.MetricsHttpServer(
                metricsPort, metrics, new com.actiongame.server.observability.AlertEvaluator());
        metricsServer.start();
        log.info("Metrics server listening on http://0.0.0.0:{}/metrics", metricsServer.getPort());
```

在关闭钩子中新增：

```java
            metricsServer.stop();
```

- [ ] **Step 2: 编译验证**

Run: `mvn -q -f server/pom.xml compile`

Expected: BUILD SUCCESS。

- [ ] **Step 3: 自查**

检查点：
- 指标服务与 WebSocket 服务共用同一份 `ServerMetrics`，数据一致。
- `METRICS_PORT` 可配置，默认 9091。
- 关闭钩子会停止指标服务，避免端口残留。

- [ ] **Step 4: Commit**

```bash
git add server/src/main/java/com/actiongame/server/Bootstrap.java
git commit -m "feat: start metrics http server in bootstrap"
```

## Task 5: GitHub Actions CI

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: 创建 CI 工作流**

创建 `.github/workflows/ci.yml`：

```yaml
name: CI

on:
  push:
    branches: [master, main, "codex/**"]
  pull_request:

jobs:
  server-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: server
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: maven-${{ hashFiles('server/pom.xml') }}
          restore-keys: maven-

      - name: Run server tests
        run: mvn -B test
```

- [ ] **Step 2: 自查**

检查点：
- 只在 `server/` 目录运行测试，不依赖 Unity 或 Docker。
- 缓存 Maven 依赖，加快 CI。
- push 到 `codex/**` 分支也会触发，便于当前开发分支验证。

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add github actions maven test workflow"
```

## Task 6: 可观测性与 CI 文档

**Files:**
- Create: `docs/observability/OBSERVABILITY.md`
- Create: `docs/observability/grafana-dashboard.json`
- Create: `docs/ci-cd/CI_CD.md`

- [ ] **Step 1: 创建可观测性文档**

创建 `docs/observability/OBSERVABILITY.md`：

```markdown
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
```

- [ ] **Step 2: 创建 Grafana Dashboard 定义**

创建 `docs/observability/grafana-dashboard.json`：

```json
{
  "title": "ActionGameDemo Server",
  "panels": [
    {
      "title": "Active Connections",
      "targets": [
        {
          "expr": "actiongame_connections_active"
        }
      ]
    },
    {
      "title": "Messages Received",
      "targets": [
        {
          "expr": "rate(actiongame_messages_received_total[1m])"
        }
      ]
    },
    {
      "title": "Server Errors",
      "targets": [
        {
          "expr": "increase(actiongame_errors_total[1m])"
        }
      ]
    }
  ]
}
```

- [ ] **Step 3: 创建 CI/CD 文档**

创建 `docs/ci-cd/CI_CD.md`：

```markdown
# ActionGameDemo CI/CD

## CI

GitHub Actions 工作流：`.github/workflows/ci.yml`

触发时机：

- push 到 `master`、`main` 或 `codex/**`
- 创建 Pull Request

流程：

1. 检出代码
2. 安装 JDK 17
3. 缓存 Maven 依赖
4. 在 `server/` 目录执行 `mvn -B test`

## 后续 CD

CD 阶段可基于 Docker 镜像构建与推送，建议后续接入：

- `docker build` 多阶段构建
- 镜像版本打标签并推送到镜像仓库
- 部署到测试环境并执行健康检查
```

- [ ] **Step 4: 自查**

检查点：
- 文档中端口、指标名、告警阈值与代码一致。
- Dashboard 使用真实 PromQL 表达式。
- CI 工作流不依赖本地环境，可直接在 GitHub 运行。

- [ ] **Step 5: Commit**

```bash
git add docs/observability docs/ci-cd
git commit -m "docs: add observability and ci cd guide"
```

## Task 7: 全量验证与文档同步

**Files:**
- Modify: `README.md`
- Modify: `CODE_WIKI.md`
- Modify: `KnowledgeBase/Projects/ActionGameDemo MOC.md`
- Modify: `Vault/Tasks/ActionGameDemo_秋招准备清单.md`

- [ ] **Step 1: 更新测试数量与 P1 状态**

运行全部测试后，把 README 与秋招清单中的测试数量更新为实际数量，并将 `监控与运维`、`可观测性`、`CI/CD` 标记为已实现。

- [ ] **Step 2: 全量测试**

Run: `mvn -q -f server/pom.xml test`

Expected: BUILD SUCCESS。

- [ ] **Step 3: 自查**

检查点：
- 文档 P1 状态与代码实现一致。
- 没有触碰 Unity 美术/客户端表现代码。
- CI 与指标服务不依赖本地 Docker 或外部账号。

- [ ] **Step 4: Commit**

```bash
git add README.md CODE_WIKI.md "KnowledgeBase/Projects/ActionGameDemo MOC.md" "Vault/Tasks/ActionGameDemo_秋招准备清单.md"
git commit -m "docs: mark phase 4 observability and ci as implemented"
```
