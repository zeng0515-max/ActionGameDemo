# ActionGameDemo 后端化改造 Phase 3 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Realtime Combat Server 增加可复用的压测工具：真实 WebSocket 客户端并发登录、加入房间、持续上报操作，统计帧广播、操作 RTT、字节速率与进程 CPU，并输出 benchmark 报告。

**Architecture:** 压测客户端复用服务端 `MessageEncoder`/`MessageDecoder` 与自定义二进制协议，通过 Netty WebSocket 连接本地启动的 `WebSocketServer`。每个客户端属于一个房间，按固定间隔发送 `PlayerActionReq`，统计 `BattleFrameNotify` 与 `PlayerActionResp`。`BenchmarkRunner` 在单进程内启动服务端并执行压测，把结果写入 Markdown 报告。

**Tech Stack:** Java 17、Netty 4.1、Maven Exec Plugin、JUnit 5、Markdown。

---

## 范围说明

Phase 3 交付：玩家房间查找性能修复、压测协议工具、Netty 压测客户端、BenchmarkRunner、真实 benchmark 报告。不修改客户端 Unity 代码，不修改 MySQL/Redis 持久化实现。

## 文件结构

- Modify: `server/src/main/java/com/actiongame/server/net/session/GameSession.java`
- Modify: `server/src/main/java/com/actiongame/server/net/handler/JoinRoomHandler.java`
- Modify: `server/src/main/java/com/actiongame/server/net/handler/PlayerActionHandler.java`
- Modify: `server/src/main/java/com/actiongame/server/net/WebSocketServer.java`
- Modify: `server/pom.xml`
- Create: `server/src/main/java/com/actiongame/server/benchmark/LoadTestPayloads.java`
- Create: `server/src/main/java/com/actiongame/server/benchmark/LoadTestConfig.java`
- Create: `server/src/main/java/com/actiongame/server/benchmark/LoadTestClientStats.java`
- Create: `server/src/main/java/com/actiongame/server/benchmark/LoadTestClient.java`
- Create: `server/src/main/java/com/actiongame/server/benchmark/BenchmarkReport.java`
- Create: `server/src/main/java/com/actiongame/server/benchmark/BenchmarkRunner.java`
- Create: `server/src/test/java/com/actiongame/server/net/session/GameSessionRoomIdTest.java`
- Create: `server/src/test/java/com/actiongame/server/benchmark/LoadTestPayloadsTest.java`
- Create: `server/src/test/java/com/actiongame/server/benchmark/LoadTestClientStatsTest.java`
- Create: `server/src/test/java/com/actiongame/server/benchmark/BenchmarkReportTest.java`

## Task 1: 修复玩家到房间的 O(n) 查找

**Files:**
- Modify: `server/src/main/java/com/actiongame/server/net/session/GameSession.java`
- Modify: `server/src/main/java/com/actiongame/server/net/handler/JoinRoomHandler.java`
- Modify: `server/src/main/java/com/actiongame/server/net/handler/PlayerActionHandler.java`
- Test: `server/src/test/java/com/actiongame/server/net/session/GameSessionRoomIdTest.java`

- [ ] **Step 1: 先写失败测试**

创建 `server/src/test/java/com/actiongame/server/net/session/GameSessionRoomIdTest.java`：

```java
package com.actiongame.server.net.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameSession 房间归属")
class GameSessionRoomIdTest {

    @Test
    @DisplayName("should_returnNull_when_noRoomJoined")
    void should_returnNull_when_noRoomJoined() {
        GameSession session = new GameSession("p1", null);

        assertThat(session.getRoomId()).isNull();
    }

    @Test
    @DisplayName("should_setAndGetRoomId_when_roomJoined")
    void should_setAndGetRoomId_when_roomJoined() {
        GameSession session = new GameSession("p1", null);

        session.setRoomId("room-1");

        assertThat(session.getRoomId()).isEqualTo("room-1");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -f server/pom.xml test -Dtest=GameSessionRoomIdTest`

Expected: FAIL，原因是 `getRoomId`/`setRoomId` 不存在。

- [ ] **Step 3: 在 GameSession 中增加 roomId**

在 `GameSession` 的 `negotiatedVersion` 字段后新增：

```java
    /** 玩家当前所在房间 */
    private volatile String roomId;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
```

- [ ] **Step 4: 在 JoinRoomHandler 记录房间**

在 `JoinRoomHandler.handle` 中，`sendJoinRoomResp(ctx, wrapper, 0, roomId, entityId, ...)` 调用前新增：

```java
        session.setRoomId(roomId);
```

- [ ] **Step 5: 将 PlayerActionHandler 改为 O(1) 查房**

将 `PlayerActionHandler.findRoomForPlayer`：

```java
    private BattleRoom findRoomForPlayer(String playerId) {
        for (BattleRoom room : RoomManager.getInstance().getAllRooms()) {
            for (var rp : room.getPlayers()) {
                if (rp.getPlayerId().equals(playerId)) {
                    return room;
                }
            }
        }
        return null;
    }
```

替换为：

```java
    private BattleRoom findRoomForPlayer(String playerId) {
        GameSession session = connectionManager.getSession(playerId);
        if (session == null) return null;
        String roomId = session.getRoomId();
        if (roomId == null) return null;
        return RoomManager.getInstance().getRoom(roomId);
    }
```

- [ ] **Step 6: 运行测试与编译验证**

Run: `mvn -q -f server/pom.xml test -Dtest=GameSessionRoomIdTest`

Expected: PASS，2 个测试通过。

Run: `mvn -q -f server/pom.xml compile`

Expected: BUILD SUCCESS。

- [ ] **Step 7: 自查**

检查点：
- 不再遍历 `getAllRooms()`，压测时操作路由不会随房间数线性退化。
- `session.setRoomId` 只发生在加入成功后。
- 未引入外部依赖或协议变更。

- [ ] **Step 8: Commit**

```bash
git add server/src/main/java/com/actiongame/server/net server/src/test/java/com/actiongame/server/net/session/GameSessionRoomIdTest.java
git commit -m "perf: route player actions by session room id"
```

## Task 2: 压测协议工具与统计模型

**Files:**
- Create: `server/src/main/java/com/actiongame/server/benchmark/LoadTestPayloads.java`
- Create: `server/src/main/java/com/actiongame/server/benchmark/LoadTestConfig.java`
- Create: `server/src/main/java/com/actiongame/server/benchmark/LoadTestClientStats.java`
- Create: `server/src/main/java/com/actiongame/server/benchmark/BenchmarkReport.java`
- Test: `server/src/test/java/com/actiongame/server/benchmark/LoadTestPayloadsTest.java`
- Test: `server/src/test/java/com/actiongame/server/benchmark/LoadTestClientStatsTest.java`
- Test: `server/src/test/java/com/actiongame/server/benchmark/BenchmarkReportTest.java`

- [ ] **Step 1: 先写失败测试**

创建 `server/src/test/java/com/actiongame/server/benchmark/LoadTestPayloadsTest.java`：

```java
package com.actiongame.server.benchmark;

import com.actiongame.server.net.util.BinaryCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("压测协议负载")
class LoadTestPayloadsTest {

    @Test
    @DisplayName("should_buildLoginPayload_when_called")
    void should_buildLoginPayload_when_called() {
        byte[] payload = LoadTestPayloads.buildLoginPayload("token-1", "bench-0");

        int offset = 0;
        int version = BinaryCodec.readInt(payload, offset); offset += 4;
        String token = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(token);
        String playerName = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(playerName);
        int configId = BinaryCodec.readInt(payload, offset);

        assertThat(version).isEqualTo(10);
        assertThat(token).isEqualTo("token-1");
        assertThat(playerName).isEqualTo("bench-0");
        assertThat(configId).isEqualTo(1);
    }

    @Test
    @DisplayName("should_buildJoinPayload_when_called")
    void should_buildJoinPayload_when_called() {
        byte[] payload = LoadTestPayloads.buildJoinRoomPayload("bench-0", "room-0");

        int offset = 0;
        int version = BinaryCodec.readInt(payload, offset); offset += 4;
        String playerId = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(playerId);
        String roomId = BinaryCodec.readString(payload, offset);

        assertThat(version).isEqualTo(10);
        assertThat(playerId).isEqualTo("bench-0");
        assertThat(roomId).isEqualTo("room-0");
    }

    @Test
    @DisplayName("should_buildActionPayload_when_called")
    void should_buildActionPayload_when_called() {
        byte[] payload = LoadTestPayloads.buildActionPayload(7, 99L, 1);

        int offset = 0;
        int version = BinaryCodec.readInt(payload, offset); offset += 4;
        int entityId = BinaryCodec.readInt(payload, offset); offset += 4;
        long frameIndex = BinaryCodec.readLong(payload, offset); offset += 8;
        int actionCount = BinaryCodec.readInt(payload, offset); offset += 4;
        int actionType = payload[offset] & 0xFF;

        assertThat(version).isEqualTo(10);
        assertThat(entityId).isEqualTo(7);
        assertThat(frameIndex).isEqualTo(99L);
        assertThat(actionCount).isEqualTo(1);
        assertThat(actionType).isEqualTo(1);
    }

    @Test
    @DisplayName("should_parseJoinResponse_when_called")
    void should_parseJoinResponse_when_called() {
        byte[] payload = new byte[4 + 4 + BinaryCodec.stringSize("room-0") + 4 + 4];
        int offset = 0;
        BinaryCodec.writeInt(payload, offset, 10); offset += 4;
        BinaryCodec.writeInt(payload, offset, 0); offset += 4;
        BinaryCodec.writeString(payload, offset, "room-0"); offset += BinaryCodec.stringSize("room-0");
        BinaryCodec.writeInt(payload, offset, 7); offset += 4;
        BinaryCodec.writeInt(payload, offset, 0);

        LoadTestPayloads.JoinResult result = LoadTestPayloads.parseJoinResponse(payload);

        assertThat(result.code()).isZero();
        assertThat(result.roomId()).isEqualTo("room-0");
        assertThat(result.entityId()).isEqualTo(7);
    }
}
```

创建 `server/src/test/java/com/actiongame/server/benchmark/LoadTestClientStatsTest.java`：

```java
package com.actiongame.server.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("压测客户端统计")
class LoadTestClientStatsTest {

    @Test
    @DisplayName("should_averageRtt_when_rttRecorded")
    void should_averageRtt_when_rttRecorded() {
        LoadTestClientStats stats = new LoadTestClientStats("bench-0");

        stats.recordActionRtt(2);
        stats.recordActionRtt(6);

        assertThat(stats.getAverageActionRttMs()).isEqualTo(4.0);
        assertThat(stats.getActionRttCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_countFramesActionsAndBytes_when_recorded")
    void should_countFramesActionsAndBytes_when_recorded() {
        LoadTestClientStats stats = new LoadTestClientStats("bench-0");

        stats.recordFrame();
        stats.recordFrame();
        stats.recordActionSent();
        stats.recordBytesReceived(100);
        stats.recordBytesSent(50);
        stats.recordError();

        assertThat(stats.getFramesReceived()).isEqualTo(2);
        assertThat(stats.getActionsSent()).isEqualTo(1);
        assertThat(stats.getBytesReceived()).isEqualTo(100);
        assertThat(stats.getBytesSent()).isEqualTo(50);
        assertThat(stats.getErrors()).isEqualTo(1);
    }
}
```

创建 `server/src/test/java/com/actiongame/server/benchmark/BenchmarkReportTest.java`：

```java
package com.actiongame.server.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Benchmark 报告")
class BenchmarkReportTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should_writeMarkdown_when_reportCreated")
    void should_writeMarkdown_when_reportCreated() throws Exception {
        LoadTestConfig config = new LoadTestConfig(4, 1000L, 50, 4, "127.0.0.1", 9090);
        LoadTestClientStats stats = new LoadTestClientStats("bench-0");
        stats.recordFrame();
        stats.recordActionSent();
        stats.recordActionRtt(3);
        stats.recordBytesReceived(1024);
        stats.recordBytesSent(256);
        BenchmarkReport report = new BenchmarkReport(config, List.of(stats), 1000L, 0.12);

        Path file = tempDir.resolve("benchmark.md");
        report.writeTo(file);

        String content = Files.readString(file);
        assertThat(content).contains("# Benchmark Report");
        assertThat(content).contains("Average action RTT");
        assertThat(content).contains("Avg process CPU load");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -f server/pom.xml test '-Dtest=LoadTestPayloadsTest,LoadTestClientStatsTest,BenchmarkReportTest'`

Expected: FAIL，原因是 benchmark 类不存在。

- [ ] **Step 3: 实现协议工具**

创建 `server/src/main/java/com/actiongame/server/benchmark/LoadTestPayloads.java`：

```java
package com.actiongame.server.benchmark;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.net.util.BinaryCodec;

public final class LoadTestPayloads {
    private LoadTestPayloads() {}

    public static byte[] buildLoginPayload(String token, String playerName) {
        int size = 4 + BinaryCodec.stringSize(token) + BinaryCodec.stringSize(playerName) + 4;
        byte[] payload = new byte[size];
        int offset = 0;
        BinaryCodec.writeInt(payload, offset, GameConstants.PROTOCOL_VERSION); offset += 4;
        BinaryCodec.writeString(payload, offset, token); offset += BinaryCodec.stringSize(token);
        BinaryCodec.writeString(payload, offset, playerName); offset += BinaryCodec.stringSize(playerName);
        BinaryCodec.writeInt(payload, offset, 1);
        return payload;
    }

    public static byte[] buildJoinRoomPayload(String playerId, String roomId) {
        int size = 4 + BinaryCodec.stringSize(playerId) + BinaryCodec.stringSize(roomId);
        byte[] payload = new byte[size];
        int offset = 0;
        BinaryCodec.writeInt(payload, offset, GameConstants.PROTOCOL_VERSION); offset += 4;
        BinaryCodec.writeString(payload, offset, playerId); offset += BinaryCodec.stringSize(playerId);
        BinaryCodec.writeString(payload, offset, roomId);
        return payload;
    }

    public static byte[] buildActionPayload(int entityId, long clientFrameIndex, int actionType) {
        int actionBytes = 1 + 4 + 4 + 4 + 4 + 1 + 4 + 8;
        int size = 4 + 4 + 8 + 4 + actionBytes;
        byte[] payload = new byte[size];
        int offset = 0;
        BinaryCodec.writeInt(payload, offset, GameConstants.PROTOCOL_VERSION); offset += 4;
        BinaryCodec.writeInt(payload, offset, entityId); offset += 4;
        BinaryCodec.writeLong(payload, offset, clientFrameIndex); offset += 8;
        BinaryCodec.writeInt(payload, offset, 1); offset += 4;
        payload[offset++] = (byte) actionType;
        BinaryCodec.writeInt(payload, offset, 0); offset += 4;
        BinaryCodec.writeFloat(payload, offset, 0f); offset += 4;
        BinaryCodec.writeFloat(payload, offset, 0f); offset += 4;
        BinaryCodec.writeInt(payload, offset, 0); offset += 4;
        offset += 1;
        BinaryCodec.writeInt(payload, offset, -1); offset += 4;
        BinaryCodec.writeLong(payload, offset, System.currentTimeMillis());
        return payload;
    }

    public static JoinResult parseJoinResponse(byte[] payload) {
        int offset = 0;
        int protocolVersion = BinaryCodec.readInt(payload, offset); offset += 4;
        int code = BinaryCodec.readInt(payload, offset); offset += 4;
        String roomId = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(roomId);
        int entityId = BinaryCodec.readInt(payload, offset); offset += 4;
        int startFrameIndex = BinaryCodec.readInt(payload, offset);
        return new JoinResult(protocolVersion, code, roomId, entityId, startFrameIndex);
    }

    public record JoinResult(int protocolVersion, int code, String roomId, int entityId, int startFrameIndex) {}
}
```

- [ ] **Step 4: 实现配置与统计**

创建 `server/src/main/java/com/actiongame/server/benchmark/LoadTestConfig.java`：

```java
package com.actiongame.server.benchmark;

public class LoadTestConfig {
    private final int clientCount;
    private final long durationMs;
    private final int actionIntervalMs;
    private final int playersPerRoom;
    private final String host;
    private final int port;

    public LoadTestConfig(int clientCount, long durationMs, int actionIntervalMs,
                          int playersPerRoom, String host, int port) {
        this.clientCount = clientCount;
        this.durationMs = durationMs;
        this.actionIntervalMs = actionIntervalMs;
        this.playersPerRoom = playersPerRoom;
        this.host = host;
        this.port = port;
    }

    public int getClientCount() { return clientCount; }
    public long getDurationMs() { return durationMs; }
    public int getActionIntervalMs() { return actionIntervalMs; }
    public int getPlayersPerRoom() { return playersPerRoom; }
    public String getHost() { return host; }
    public int getPort() { return port; }

    public int getRoomCount() {
        return Math.max(1, (clientCount + playersPerRoom - 1) / playersPerRoom);
    }

    public String roomIdForClient(int index) {
        return "bench-room-" + (index / playersPerRoom);
    }
}
```

创建 `server/src/main/java/com/actiongame/server/benchmark/LoadTestClientStats.java`：

```java
package com.actiongame.server.benchmark;

import java.util.concurrent.atomic.AtomicLong;

public class LoadTestClientStats {
    private final String playerName;
    private final AtomicLong framesReceived = new AtomicLong();
    private final AtomicLong actionsSent = new AtomicLong();
    private final AtomicLong actionRttTotalMs = new AtomicLong();
    private final AtomicLong actionRttCount = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong bytesReceived = new AtomicLong();
    private final AtomicLong bytesSent = new AtomicLong();

    public LoadTestClientStats(String playerName) {
        this.playerName = playerName;
    }

    public void recordFrame() { framesReceived.incrementAndGet(); }
    public void recordActionSent() { actionsSent.incrementAndGet(); }
    public void recordActionRtt(long rttMs) {
        actionRttTotalMs.addAndGet(rttMs);
        actionRttCount.incrementAndGet();
    }
    public void recordError() { errors.incrementAndGet(); }
    public void recordBytesReceived(int bytes) { bytesReceived.addAndGet(bytes); }
    public void recordBytesSent(int bytes) { bytesSent.addAndGet(bytes); }

    public String getPlayerName() { return playerName; }
    public long getFramesReceived() { return framesReceived.get(); }
    public long getActionsSent() { return actionsSent.get(); }
    public long getActionRttTotalMs() { return actionRttTotalMs.get(); }
    public long getActionRttCount() { return actionRttCount.get(); }
    public long getErrors() { return errors.get(); }
    public long getBytesReceived() { return bytesReceived.get(); }
    public long getBytesSent() { return bytesSent.get(); }

    public double getAverageActionRttMs() {
        long count = actionRttCount.get();
        return count == 0 ? 0 : (double) actionRttTotalMs.get() / count;
    }
}
```

- [ ] **Step 5: 实现报告模型**

创建 `server/src/main/java/com/actiongame/server/benchmark/BenchmarkReport.java`：

```java
package com.actiongame.server.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class BenchmarkReport {
    private final LoadTestConfig config;
    private final List<LoadTestClientStats> results;
    private final long elapsedMs;
    private final double avgCpuLoad;

    public BenchmarkReport(LoadTestConfig config, List<LoadTestClientStats> results,
                           long elapsedMs, double avgCpuLoad) {
        this.config = config;
        this.results = results;
        this.elapsedMs = elapsedMs;
        this.avgCpuLoad = avgCpuLoad;
    }

    public String toMarkdown() {
        long totalActions = sumLong(s -> s.getActionsSent());
        long totalRttCount = sumLong(LoadTestClientStats::getActionRttCount);
        long totalRttMs = sumLong(LoadTestClientStats::getActionRttTotalMs);
        long totalFrames = sumLong(LoadTestClientStats::getFramesReceived);
        long totalErrors = sumLong(LoadTestClientStats::getErrors);
        long totalBytesReceived = sumLong(LoadTestClientStats::getBytesReceived);
        long totalBytesSent = sumLong(LoadTestClientStats::getBytesSent);
        double avgRtt = totalRttCount == 0 ? 0 : (double) totalRttMs / totalRttCount;
        double seconds = Math.max(1, elapsedMs) / 1000.0;

        return "# Benchmark Report\n\n"
            + "- Date: " + LocalDateTime.now() + "\n"
            + "- Host: " + config.getHost() + ":" + config.getPort() + "\n"
            + "- Clients: " + config.getClientCount() + "\n"
            + "- Rooms: " + config.getRoomCount() + "\n"
            + "- Duration: " + config.getDurationMs() + " ms\n"
            + "- Action interval: " + config.getActionIntervalMs() + " ms\n"
            + "- Elapsed: " + elapsedMs + " ms\n"
            + "- Avg process CPU load: " + String.format("%.1f%%", avgCpuLoad * 100) + "\n\n"
            + "| Metric | Value |\n"
            + "|---|---|\n"
            + "| Actions sent | " + totalActions + " |\n"
            + "| Action responses | " + totalRttCount + " |\n"
            + "| Average action RTT | " + String.format("%.2f ms", avgRtt) + " |\n"
            + "| Frame notifications received | " + totalFrames + " |\n"
            + "| Frame notifications received / client | "
            + String.format("%.1f", results.isEmpty() ? 0 : (double) totalFrames / results.size()) + " |\n"
            + "| Frame notifications / second | " + String.format("%.1f", totalFrames / seconds) + " |\n"
            + "| Bytes received | " + totalBytesReceived + " |\n"
            + "| Bytes sent | " + totalBytesSent + " |\n"
            + "| Received rate | " + String.format("%.1f KB/s", totalBytesReceived / 1024.0 / seconds) + " |\n"
            + "| Sent rate | " + String.format("%.1f KB/s", totalBytesSent / 1024.0 / seconds) + " |\n"
            + "| Errors | " + totalErrors + " |\n";
    }

    public void writeTo(Path output) throws IOException {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.writeString(output, toMarkdown());
    }

    private long sumLong(java.util.function.ToLongFunction<LoadTestClientStats> mapper) {
        return results.stream().mapToLong(mapper).sum();
    }
}
```

- [ ] **Step 6: 运行测试确认通过**

Run: `mvn -q -f server/pom.xml test '-Dtest=LoadTestPayloadsTest,LoadTestClientStatsTest,BenchmarkReportTest'`

Expected: PASS，全部通过。

- [ ] **Step 7: 自查**

检查点：
- 负载格式与 `LoginHandler`/`JoinRoomHandler`/`PlayerActionHandler` 的二进制解析完全一致。
- 统计使用原子类，压测多线程并发计数不会丢失。
- 报告不依赖外部数据库，纯内存聚合。

- [ ] **Step 8: Commit**

```bash
git add server/src/main/java/com/actiongame/server/benchmark server/src/test/java/com/actiongame/server/benchmark
git commit -m "feat: add benchmark protocol tools and stats model"
```

## Task 3: Netty 压测客户端与 BenchmarkRunner

**Files:**
- Modify: `server/src/main/java/com/actiongame/server/net/WebSocketServer.java`
- Modify: `server/pom.xml`
- Create: `server/src/main/java/com/actiongame/server/benchmark/LoadTestClient.java`
- Create: `server/src/main/java/com/actiongame/server/benchmark/BenchmarkRunner.java`

- [ ] **Step 1: 给 WebSocketServer 增加端口查询**

在 `WebSocketServer` 中新增：

```java
    public int getPort() {
        if (serverChannel == null) {
            throw new IllegalStateException("Server not started");
        }
        return ((java.net.InetSocketAddress) serverChannel.localAddress()).getPort();
    }
```

- [ ] **Step 2: 添加 Maven Exec 插件**

在 `server/pom.xml` 的 `<plugins>` 中新增：

```xml
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>com.actiongame.server.benchmark.BenchmarkRunner</mainClass>
                </configuration>
            </plugin>
```

- [ ] **Step 3: 实现压测客户端**

创建 `server/src/main/java/com/actiongame/server/benchmark/LoadTestClient.java`：

```java
package com.actiongame.server.benchmark;

import com.actiongame.server.net.codec.MessageDecoder;
import com.actiongame.server.net.codec.MessageEncoder;
import com.actiongame.server.net.util.BinaryCodec;
import com.actiongame.server.net.util.MessageHelper;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestClient {
    private final LoadTestConfig config;
    private final String playerName;
    private final String roomId;
    private final LoadTestClientStats stats;
    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<Long, Long> pendingActionTimestamps = new ConcurrentHashMap<>();

    private volatile int entityId = -1;
    private volatile Channel channel;

    public LoadTestClient(LoadTestConfig config, int clientIndex) {
        this.config = config;
        this.playerName = "bench-" + clientIndex;
        this.roomId = config.roomIdForClient(clientIndex);
        this.stats = new LoadTestClientStats(playerName);
    }

    public LoadTestClientStats run(NioEventLoopGroup group) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    URI uri = URI.create("ws://" + config.getHost() + ":" + config.getPort() + "/game");
                    WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
                    ch.pipeline().addLast(new HttpClientCodec());
                    ch.pipeline().addLast(new HttpObjectAggregator(65536));
                    ch.pipeline().addLast(new WebSocketClientProtocolHandler(handshaker));
                    ch.pipeline().addLast(new ByteCountingHandler());
                    ch.pipeline().addLast(new MessageDecoder());
                    ch.pipeline().addLast(new MessageEncoder());
                    ch.pipeline().addLast(new ClientHandler());
                }
            });

        channel = bootstrap.connect(config.getHost(), config.getPort()).sync().channel();
        boolean ready = readyLatch.await(10, TimeUnit.SECONDS);
        if (!ready || channel == null || !channel.isActive()) {
            stats.recordError();
            closeQuietly();
            return stats;
        }

        long endTime = System.currentTimeMillis() + config.getDurationMs();
        long frameIndex = 0;
        while (System.currentTimeMillis() < endTime && channel.isActive()) {
            long seq = sequence.incrementAndGet();
            long sentAt = System.currentTimeMillis();
            pendingActionTimestamps.put(seq, sentAt);
            channel.writeAndFlush(buildActionMessage(seq, frameIndex++));
            stats.recordActionSent();
            Thread.sleep(config.getActionIntervalMs());
        }

        closeQuietly();
        return stats;
    }

    private MessageWrapper buildActionMessage(long seq, long frameIndex) {
        byte[] payload = LoadTestPayloads.buildActionPayload(entityId, frameIndex, 1);
        return MessageHelper.wrap(MessageId.PLAYER_ACTION_REQ, seq, payload);
    }

    private void closeQuietly() {
        if (channel != null) {
            channel.close().addListener(ChannelFutureListener.CLOSE);
        }
    }

    private class ClientHandler extends SimpleChannelInboundHandler<MessageWrapper> {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                byte[] login = LoadTestPayloads.buildLoginPayload("bench-token", playerName);
                ctx.writeAndFlush(MessageHelper.wrap(MessageId.LOGIN_REQ, sequence.incrementAndGet(), login));
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MessageWrapper msg) {
            switch (msg.getMessageId()) {
                case LOGIN_RESP -> {
                    byte[] payload = msg.getPayload().toByteArray();
                    int code = BinaryCodec.readInt(payload, 4);
                    if (code != 0) {
                        stats.recordError();
                        readyLatch.countDown();
                        return;
                    }
                    byte[] join = LoadTestPayloads.buildJoinRoomPayload(playerName, roomId);
                    ctx.writeAndFlush(MessageHelper.wrap(MessageId.JOIN_ROOM_REQ, sequence.incrementAndGet(), join));
                }
                case JOIN_ROOM_RESP -> {
                    LoadTestPayloads.JoinResult result = LoadTestPayloads.parseJoinResponse(msg.getPayload().toByteArray());
                    if (result.code() != 0) {
                        stats.recordError();
                        readyLatch.countDown();
                        return;
                    }
                    entityId = result.entityId();
                    readyLatch.countDown();
                }
                case BATTLE_FRAME_NOTIFY -> stats.recordFrame();
                case PLAYER_ACTION_RESP -> {
                    Long sentAt = pendingActionTimestamps.remove(msg.getSequenceId());
                    if (sentAt != null) {
                        stats.recordActionRtt(System.currentTimeMillis() - sentAt);
                    }
                }
                default -> {}
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            readyLatch.countDown();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            stats.recordError();
            ctx.close();
        }
    }

    private class ByteCountingHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof BinaryWebSocketFrame frame) {
                stats.recordBytesReceived(frame.content().readableBytes());
            }
            ctx.fireChannelRead(msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof BinaryWebSocketFrame frame) {
                stats.recordBytesSent(frame.content().readableBytes());
            }
            ctx.write(msg, promise);
        }
    }
}
```

- [ ] **Step 4: 实现 BenchmarkRunner**

创建 `server/src/main/java/com/actiongame/server/benchmark/BenchmarkRunner.java`：

```java
package com.actiongame.server.benchmark;

import com.actiongame.server.audit.AuditLoggerImpl;
import com.actiongame.server.net.WebSocketServer;
import com.actiongame.server.net.handler.HandlerRegistry;
import com.actiongame.server.net.handler.HeartbeatHandler;
import com.actiongame.server.net.handler.JoinRoomHandler;
import com.actiongame.server.net.handler.LoginHandler;
import com.actiongame.server.net.handler.PlayerActionHandler;
import com.actiongame.server.net.session.ConnectionManager;
import io.netty.channel.nio.NioEventLoopGroup;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class BenchmarkRunner {
    private BenchmarkRunner() {}

    public static void main(String[] args) throws Exception {
        int clients = args.length > 0 ? Integer.parseInt(args[0]) : 16;
        long durationSec = args.length > 1 ? Long.parseLong(args[1]) : 10;
        int actionIntervalMs = args.length > 2 ? Integer.parseInt(args[2]) : 50;
        int port = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        String outputPath = args.length > 4 ? args[4]
            : Path.of("docs", "benchmark", "BENCHMARK_" + LocalDate.now() + ".md").toString();

        BenchmarkReport report = run(clients, durationSec, actionIntervalMs, port);
        report.writeTo(Path.of(outputPath));
        System.out.println(report.toMarkdown());
        System.out.println("Report written to " + outputPath);
    }

    public static BenchmarkReport run(int clients, long durationSec, int actionIntervalMs, int port)
            throws Exception {
        ConnectionManager connectionManager = new ConnectionManager();
        HandlerRegistry registry = new HandlerRegistry(connectionManager);
        AuditLoggerImpl auditLogger = new AuditLoggerImpl(
            Path.of(System.getProperty("java.io.tmpdir"), "actiongame-benchmark-logs"));
        registry.register(new LoginHandler(connectionManager, auditLogger));
        registry.register(new HeartbeatHandler());
        registry.register(new JoinRoomHandler(connectionManager, auditLogger));
        registry.register(new PlayerActionHandler(connectionManager, auditLogger));

        WebSocketServer server = new WebSocketServer(port, connectionManager, registry);
        server.start();
        int actualPort = server.getPort();

        LoadTestConfig config = new LoadTestConfig(
            clients, durationSec * 1000L, actionIntervalMs, 4, "127.0.0.1", actualPort);
        NioEventLoopGroup group = new NioEventLoopGroup();
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(2, clients));

        var cpuSum = new AtomicLong();
        var cpuSamples = new AtomicLong();
        var os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Thread sampler = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                double load = os.getProcessCpuLoad();
                if (load >= 0) {
                    cpuSum.addAndGet((long) (load * 1000));
                    cpuSamples.incrementAndGet();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        sampler.setDaemon(true);
        sampler.start();

        long start = System.currentTimeMillis();
        List<Future<LoadTestClientStats>> futures = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            LoadTestClient client = new LoadTestClient(config, i);
            futures.add(executor.submit(() -> client.run(group)));
        }

        List<LoadTestClientStats> results = new ArrayList<>();
        for (Future<LoadTestClientStats> future : futures) {
            results.add(future.get(60, TimeUnit.SECONDS));
        }
        long elapsed = System.currentTimeMillis() - start;

        sampler.interrupt();
        executor.shutdownNow();
        group.shutdownGracefully().sync();
        server.shutdown();

        double avgCpu = cpuSamples.get() == 0 ? 0 : cpuSum.get() / (1000.0 * cpuSamples.get());
        return new BenchmarkReport(config, results, elapsed, avgCpu);
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `mvn -q -f server/pom.xml compile`

Expected: BUILD SUCCESS。

- [ ] **Step 6: 自查**

检查点：
- 客户端走完整 `LoginReq -> LoginResp -> JoinRoomReq -> JoinRoomResp -> PlayerActionReq -> BattleFrameNotify` 流程。
- 字节统计发生在 WebSocket 二进制帧层，不是 Protobuf 内部对象大小。
- `BenchmarkRunner` 使用共享 EventLoopGroup，避免每个客户端单独创建线程组。
- 服务端关闭后 EventLoop 同步释放，避免压测进程挂住。

- [ ] **Step 7: Commit**

```bash
git add server/pom.xml server/src/main/java/com/actiongame/server/benchmark server/src/main/java/com/actiongame/server/net/WebSocketServer.java
git commit -m "feat: add netty load test client and benchmark runner"
```

## Task 4: 全量单测验证

**Files:**
- 无新增，只读验证

- [ ] **Step 1: 运行全部服务端测试**

Run: `mvn -q -f server/pom.xml test`

Expected: BUILD SUCCESS，全部测试通过。

- [ ] **Step 2: 检查逻辑回归**

检查点：
- 既有 `PlayerActionHandler` 相关测试仍通过。
- 新增的 `GameSessionRoomIdTest`、benchmark 单元测试通过。
- `RoomManager` 与持久化测试无回归。

- [ ] **Step 3: 确认无多余改动**

Run: `git status --short -- server/src/main/java/com/actiongame/server/benchmark server/src/test/java/com/actiongame/server/benchmark server/src/main/java/com/actiongame/server/net/session server/src/main/java/com/actiongame/server/net/handler server/src/main/java/com/actiongame/server/net/WebSocketServer.java`

Expected: 无未提交改动；Task 4 只做验证，不产生新文件。

## Task 5: 运行真实压测并生成报告

**Files:**
- Create: `docs/benchmark/BENCHMARK_2026-08-10.md`

- [ ] **Step 1: 运行 16 客户端压测**

Run: `mvn -q -f server/pom.xml compile org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=com.actiongame.server.benchmark.BenchmarkRunner -Dexec.args="16 10 50 0 D:/ActionGameDemo/docs/benchmark/BENCHMARK_2026-08-10.md"`

Expected: 输出报告 Markdown，包含 Actions sent、Frame notifications received、Average action RTT、Bytes 与 CPU 数据。

- [ ] **Step 2: 检查报告内容**

Run: `Get-Content -Encoding UTF8 docs/benchmark/BENCHMARK_2026-08-10.md`

Expected: 报告包含日期、客户端数、房间数、帧通知数、RTT、字节速率、CPU 指标。

- [ ] **Step 3: 自查**

检查点：
- 帧通知数 > 0，说明真实 WebSocket 链路已通。
- 错误数应远小于总操作数，否则说明压测客户端或服务端有连接级问题。
- 报告数据来自本次运行，不是写死的模板。

- [ ] **Step 4: Commit**

```bash
git add docs/benchmark/BENCHMARK_2026-08-10.md
git commit -m "docs: add phase 3 benchmark report"
```

## Task 6: 文档与一致性收尾

**Files:**
- Modify: `README.md`
- Modify: `CODE_WIKI.md`
- Modify: `KnowledgeBase/Projects/ActionGameDemo MOC.md`
- Modify: `Vault/Projects/ActionGameDemo.md`

- [ ] **Step 1: 将 P0 压测标记为已完成**

在 `README.md`、`CODE_WIKI.md`、`KnowledgeBase/Projects/ActionGameDemo MOC.md`、`Vault/Projects/ActionGameDemo.md`、`Vault/Tasks/ActionGameDemo_秋招准备清单.md` 中把 `压测与 benchmark（P0）`、`P0：...压测工具与 benchmark 报告` 等表述更新为已实现，并链接到 `docs/benchmark/BENCHMARK_2026-08-10.md`。

- [ ] **Step 2: 全量验证**

Run: `mvn -q -f server/pom.xml test`

Expected: BUILD SUCCESS。

- [ ] **Step 3: 自查**

检查点：
- 文档中的 P0 状态与实际实现一致。
- 没有引入对美术/客户端的依赖。
- 报告文件已提交，可直接用于秋招展示。

- [ ] **Step 4: Commit**

```bash
git add README.md CODE_WIKI.md "KnowledgeBase/Projects/ActionGameDemo MOC.md" "Vault/Projects/ActionGameDemo.md" "Vault/Tasks/ActionGameDemo_秋招准备清单.md"
git commit -m "docs: mark phase 3 benchmark as implemented"
```
