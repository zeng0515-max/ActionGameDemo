# ActionGameDemo 后端化改造 Phase 2 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Realtime Combat Server 增加 MySQL 对局结果持久化与 Redis 房间状态缓存，未配置外部存储时自动降级为内存实现，保证现有战斗流程不依赖数据库也能运行。

**Architecture:** 持久化层通过接口隔离。`MatchResultRepository` 提供对局结果存取，`RoomStateCache` 提供房间状态缓存；每个接口都有内存实现和 MySQL/Redis 实现。`StorageConfig` 从环境变量读取配置，`StorageFactory` 负责装配，`BattleRoom` 在战斗结束时保存结果，`RoomManager` 在创建/销毁房间时更新缓存。

**Tech Stack:** Java 17、Maven、HikariCP、MySQL Connector/J、Jedis、H2（测试）、Docker Compose。

---

## 范围说明

Phase 2 只做数据层：对局结果持久化、房间状态缓存、Docker 编排。回放文件仍保留本地文件存储，不做迁移。

## 文件结构

- Modify: `server/pom.xml`
- Create: `server/src/main/java/com/actiongame/server/persistence/PersistenceException.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/MatchResult.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/MatchResultRepository.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/InMemoryMatchResultRepository.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/JdbcMatchResultRepository.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/RoomState.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/RoomStateCache.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/InMemoryRoomStateCache.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/StringStore.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/JedisStringStore.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/RedisRoomStateCache.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/StorageConfig.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/StorageFactory.java`
- Modify: `server/src/main/java/com/actiongame/server/room/BattleRoom.java`
- Modify: `server/src/main/java/com/actiongame/server/room/RoomManager.java`
- Modify: `docker-compose.yml`
- Modify: `.env.example`
- Create: `server/src/test/java/com/actiongame/server/persistence/InMemoryMatchResultRepositoryTest.java`
- Create: `server/src/test/java/com/actiongame/server/persistence/JdbcMatchResultRepositoryTest.java`
- Create: `server/src/test/java/com/actiongame/server/persistence/StorageConfigTest.java`
- Create: `server/src/test/java/com/actiongame/server/persistence/InMemoryRoomStateCacheTest.java`
- Create: `server/src/test/java/com/actiongame/server/persistence/RedisRoomStateCacheTest.java`
- Create: `server/src/test/java/com/actiongame/server/persistence/BattleRoomPersistenceTest.java`
- Create: `server/src/test/java/com/actiongame/server/persistence/RoomManagerPersistenceTest.java`

## Task 1: 添加持久化依赖

**Files:**
- Modify: `server/pom.xml`

- [ ] **Step 1: 在 `server/pom.xml` 的 `<dependencies>` 中新增依赖**

在 `gson` 依赖后插入：

```xml
        <!-- Persistence -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>5.1.0</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.4.0</version>
        </dependency>
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
            <version>5.1.5</version>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.2.224</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: 验证依赖可解析**

Run: `mvn -q -f server/pom.xml dependency:resolve`

Expected: BUILD SUCCESS，无 `Could not resolve dependencies` 错误。

- [ ] **Step 3: Commit**

```bash
git add server/pom.xml
git commit -m "build: add persistence dependencies"
```

## Task 2: 对局结果领域模型与内存仓库

**Files:**
- Create: `server/src/main/java/com/actiongame/server/persistence/PersistenceException.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/MatchResult.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/MatchResultRepository.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/InMemoryMatchResultRepository.java`
- Test: `server/src/test/java/com/actiongame/server/persistence/InMemoryMatchResultRepositoryTest.java`

- [ ] **Step 1: 先写失败测试**

创建 `server/src/test/java/com/actiongame/server/persistence/InMemoryMatchResultRepositoryTest.java`：

```java
package com.actiongame.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("内存对局结果仓库")
class InMemoryMatchResultRepositoryTest {

    @Test
    @DisplayName("should_saveAndFind_when_matchResultSaved")
    void should_saveAndFind_when_matchResultSaved() {
        InMemoryMatchResultRepository repository = new InMemoryMatchResultRepository();

        repository.save(new MatchResult("room-1", 1000L, 5000L, 200L, 1, 2, 3, 0));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByRoomId("room-1")).hasSize(1);
        MatchResult saved = repository.findByRoomId("room-1").get(0);
        assertThat(saved.getRoomId()).isEqualTo("room-1");
        assertThat(saved.getTotalFrames()).isEqualTo(200L);
        assertThat(saved.getResult()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_returnEmpty_when_roomHasNoMatch")
    void should_returnEmpty_when_roomHasNoMatch() {
        InMemoryMatchResultRepository repository = new InMemoryMatchResultRepository();

        assertThat(repository.findByRoomId("missing")).isEmpty();
        assertThat(repository.count()).isZero();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -f server/pom.xml test -Dtest=InMemoryMatchResultRepositoryTest`

Expected: FAIL，原因是 `InMemoryMatchResultRepository` 等类不存在。

- [ ] **Step 3: 实现领域模型与仓库**

创建 `server/src/main/java/com/actiongame/server/persistence/PersistenceException.java`：

```java
package com.actiongame.server.persistence;

public class PersistenceException extends RuntimeException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/MatchResult.java`：

```java
package com.actiongame.server.persistence;

public class MatchResult {
    private final String roomId;
    private final long startTimeMs;
    private final long endTimeMs;
    private final long totalFrames;
    private final int result;
    private final int playerCount;
    private final int monsterCount;
    private final int cheatScore;

    public MatchResult(String roomId, long startTimeMs, long endTimeMs, long totalFrames,
                       int result, int playerCount, int monsterCount, int cheatScore) {
        this.roomId = roomId;
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.totalFrames = totalFrames;
        this.result = result;
        this.playerCount = playerCount;
        this.monsterCount = monsterCount;
        this.cheatScore = cheatScore;
    }

    public String getRoomId() { return roomId; }
    public long getStartTimeMs() { return startTimeMs; }
    public long getEndTimeMs() { return endTimeMs; }
    public long getTotalFrames() { return totalFrames; }
    public int getResult() { return result; }
    public int getPlayerCount() { return playerCount; }
    public int getMonsterCount() { return monsterCount; }
    public int getCheatScore() { return cheatScore; }
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/MatchResultRepository.java`：

```java
package com.actiongame.server.persistence;

import java.util.List;

public interface MatchResultRepository {
    void save(MatchResult result);
    List<MatchResult> findByRoomId(String roomId);
    long count();
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/InMemoryMatchResultRepository.java`：

```java
package com.actiongame.server.persistence;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryMatchResultRepository implements MatchResultRepository {
    private final Map<String, List<MatchResult>> byRoom = new ConcurrentHashMap<>();
    private final AtomicLong total = new AtomicLong();

    @Override
    public void save(MatchResult result) {
        byRoom.computeIfAbsent(result.getRoomId(), k -> new CopyOnWriteArrayList<>()).add(result);
        total.incrementAndGet();
    }

    @Override
    public List<MatchResult> findByRoomId(String roomId) {
        return List.copyOf(byRoom.getOrDefault(roomId, List.of()));
    }

    @Override
    public long count() {
        return total.get();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -f server/pom.xml test -Dtest=InMemoryMatchResultRepositoryTest`

Expected: PASS，2 个测试全部通过。

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/actiongame/server/persistence server/src/test/java/com/actiongame/server/persistence/InMemoryMatchResultRepositoryTest.java
git commit -m "feat: add match result repository with in-memory impl"
```

## Task 3: 房间状态缓存与 Redis 实现

**Files:**
- Create: `server/src/main/java/com/actiongame/server/persistence/RoomState.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/RoomStateCache.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/InMemoryRoomStateCache.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/StringStore.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/JedisStringStore.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/RedisRoomStateCache.java`
- Test: `server/src/test/java/com/actiongame/server/persistence/InMemoryRoomStateCacheTest.java`
- Test: `server/src/test/java/com/actiongame/server/persistence/RedisRoomStateCacheTest.java`

- [ ] **Step 1: 先写失败测试**

创建 `server/src/test/java/com/actiongame/server/persistence/InMemoryRoomStateCacheTest.java`：

```java
package com.actiongame.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("内存房间状态缓存")
class InMemoryRoomStateCacheTest {

    @Test
    @DisplayName("should_putGetRemove_when_stateManaged")
    void should_putGetRemove_when_stateManaged() {
        InMemoryRoomStateCache cache = new InMemoryRoomStateCache();
        RoomState state = new RoomState("room-1", "BATTLE", 2, 3, 100L, 5000L);

        cache.put(state.getRoomId(), state);

        RoomState loaded = cache.get("room-1");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getStatus()).isEqualTo("BATTLE");
        assertThat(loaded.getPlayerCount()).isEqualTo(2);

        cache.remove("room-1");
        assertThat(cache.get("room-1")).isNull();
    }
}
```

创建 `server/src/test/java/com/actiongame/server/persistence/RedisRoomStateCacheTest.java`：

```java
package com.actiongame.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Redis 房间状态缓存")
class RedisRoomStateCacheTest {

    @Test
    @DisplayName("should_putGetRemove_when_stringStoreBacked")
    void should_putGetRemove_when_stringStoreBacked() {
        FakeStringStore store = new FakeStringStore();
        RedisRoomStateCache cache = new RedisRoomStateCache(store);
        RoomState state = new RoomState("room-1", "WAITING", 0, 0, 0L, 1000L);

        cache.put(state.getRoomId(), state);

        RoomState loaded = cache.get("room-1");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getRoomId()).isEqualTo("room-1");
        assertThat(loaded.getStatus()).isEqualTo("WAITING");

        cache.remove("room-1");
        assertThat(cache.get("room-1")).isNull();
    }

    private static class FakeStringStore implements StringStore {
        private final Map<String, String> data = new HashMap<>();

        @Override
        public void set(String key, String value, int ttlSeconds) {
            data.put(key, value);
        }

        @Override
        public String get(String key) {
            return data.get(key);
        }

        @Override
        public void del(String key) {
            data.remove(key);
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -f server/pom.xml test -Dtest=InMemoryRoomStateCacheTest,RedisRoomStateCacheTest`

Expected: FAIL，原因是 `RoomState`、`RoomStateCache` 等类不存在。

- [ ] **Step 3: 实现房间状态与缓存**

创建 `server/src/main/java/com/actiongame/server/persistence/RoomState.java`：

```java
package com.actiongame.server.persistence;

public class RoomState {
    private String roomId;
    private String status;
    private int playerCount;
    private int monsterCount;
    private long currentFrameIndex;
    private long updatedAtMs;

    public RoomState() {}

    public RoomState(String roomId, String status, int playerCount, int monsterCount,
                     long currentFrameIndex, long updatedAtMs) {
        this.roomId = roomId;
        this.status = status;
        this.playerCount = playerCount;
        this.monsterCount = monsterCount;
        this.currentFrameIndex = currentFrameIndex;
        this.updatedAtMs = updatedAtMs;
    }

    public String getRoomId() { return roomId; }
    public String getStatus() { return status; }
    public int getPlayerCount() { return playerCount; }
    public int getMonsterCount() { return monsterCount; }
    public long getCurrentFrameIndex() { return currentFrameIndex; }
    public long getUpdatedAtMs() { return updatedAtMs; }
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/RoomStateCache.java`：

```java
package com.actiongame.server.persistence;

public interface RoomStateCache {
    void put(String roomId, RoomState state);
    RoomState get(String roomId);
    void remove(String roomId);
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/InMemoryRoomStateCache.java`：

```java
package com.actiongame.server.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRoomStateCache implements RoomStateCache {
    private final Map<String, RoomState> states = new ConcurrentHashMap<>();

    @Override
    public void put(String roomId, RoomState state) {
        states.put(roomId, state);
    }

    @Override
    public RoomState get(String roomId) {
        return states.get(roomId);
    }

    @Override
    public void remove(String roomId) {
        states.remove(roomId);
    }
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/StringStore.java`：

```java
package com.actiongame.server.persistence;

public interface StringStore {
    void set(String key, String value, int ttlSeconds);
    String get(String key);
    void del(String key);
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/JedisStringStore.java`：

```java
package com.actiongame.server.persistence;

import redis.clients.jedis.JedisPool;

public class JedisStringStore implements StringStore {
    private final JedisPool pool;
    private final int ttlSeconds;

    public JedisStringStore(JedisPool pool, int ttlSeconds) {
        this.pool = pool;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public void set(String key, String value, int ttlSeconds) {
        try (var jedis = pool.getResource()) {
            jedis.setex(key, ttlSeconds, value);
        }
    }

    @Override
    public String get(String key) {
        try (var jedis = pool.getResource()) {
            return jedis.get(key);
        }
    }

    @Override
    public void del(String key) {
        try (var jedis = pool.getResource()) {
            jedis.del(key);
        }
    }
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/RedisRoomStateCache.java`：

```java
package com.actiongame.server.persistence;

import com.google.gson.Gson;

public class RedisRoomStateCache implements RoomStateCache {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_TTL_SECONDS = 1800;
    private static final String KEY_PREFIX = "room:";

    private final StringStore store;

    public RedisRoomStateCache(StringStore store) {
        this.store = store;
    }

    @Override
    public void put(String roomId, RoomState state) {
        store.set(KEY_PREFIX + roomId, GSON.toJson(state), DEFAULT_TTL_SECONDS);
    }

    @Override
    public RoomState get(String roomId) {
        String json = store.get(KEY_PREFIX + roomId);
        return json == null ? null : GSON.fromJson(json, RoomState.class);
    }

    @Override
    public void remove(String roomId) {
        store.del(KEY_PREFIX + roomId);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -f server/pom.xml test -Dtest=InMemoryRoomStateCacheTest,RedisRoomStateCacheTest`

Expected: PASS，2 个测试全部通过。

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/actiongame/server/persistence server/src/test/java/com/actiongame/server/persistence
git commit -m "feat: add room state cache with redis impl"
```

## Task 4: JDBC 仓库与存储配置

**Files:**
- Create: `server/src/main/java/com/actiongame/server/persistence/JdbcMatchResultRepository.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/StorageConfig.java`
- Create: `server/src/main/java/com/actiongame/server/persistence/StorageFactory.java`
- Test: `server/src/test/java/com/actiongame/server/persistence/JdbcMatchResultRepositoryTest.java`
- Test: `server/src/test/java/com/actiongame/server/persistence/StorageConfigTest.java`

- [ ] **Step 1: 先写失败测试**

创建 `server/src/test/java/com/actiongame/server/persistence/JdbcMatchResultRepositoryTest.java`：

```java
package com.actiongame.server.persistence;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JDBC 对局结果仓库")
class JdbcMatchResultRepositoryTest {

    @Test
    @DisplayName("should_saveAndFind_when_h2Backed")
    void should_saveAndFind_when_h2Backed() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:match;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        JdbcMatchResultRepository repository = new JdbcMatchResultRepository(dataSource);
        repository.save(new MatchResult("room-jdbc", 100L, 500L, 20L, 2, 1, 4, 3));

        assertThat(repository.count()).isEqualTo(1);
        MatchResult saved = repository.findByRoomId("room-jdbc").get(0);
        assertThat(saved.getResult()).isEqualTo(2);
        assertThat(saved.getCheatScore()).isEqualTo(3);
    }
}
```

创建 `server/src/test/java/com/actiongame/server/persistence/StorageConfigTest.java`：

```java
package com.actiongame.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("存储配置")
class StorageConfigTest {

    @Test
    @DisplayName("should_defaultToInMemory_when_noEnv")
    void should_defaultToInMemory_when_noEnv() {
        StorageConfig config = new StorageConfig(Map.of());

        assertThat(config.isMySqlEnabled()).isFalse();
        assertThat(config.isRedisEnabled()).isFalse();
    }

    @Test
    @DisplayName("should_enableStores_when_configured")
    void should_enableStores_when_configured() {
        StorageConfig config = new StorageConfig(Map.of(
            "MATCH_DB_URL", "jdbc:mysql://localhost:3306/actiongame",
            "REDIS_HOST", "localhost",
            "REDIS_PORT", "6379"
        ));

        assertThat(config.isMySqlEnabled()).isTrue();
        assertThat(config.getMatchDbUrl()).isEqualTo("jdbc:mysql://localhost:3306/actiongame");
        assertThat(config.isRedisEnabled()).isTrue();
        assertThat(config.getRedisPort()).isEqualTo(6379);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -f server/pom.xml test -Dtest=JdbcMatchResultRepositoryTest,StorageConfigTest`

Expected: FAIL，原因是 `JdbcMatchResultRepository`、`StorageConfig` 不存在。

- [ ] **Step 3: 实现 JDBC 仓库与配置**

创建 `server/src/main/java/com/actiongame/server/persistence/JdbcMatchResultRepository.java`：

```java
package com.actiongame.server.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcMatchResultRepository implements MatchResultRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcMatchResultRepository.class);

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS battle_results (" +
        "room_id VARCHAR(64) NOT NULL, " +
        "start_time_ms BIGINT NOT NULL, " +
        "end_time_ms BIGINT NOT NULL, " +
        "total_frames BIGINT NOT NULL, " +
        "result INT NOT NULL, " +
        "player_count INT NOT NULL, " +
        "monster_count INT NOT NULL, " +
        "cheat_score INT NOT NULL, " +
        "PRIMARY KEY (room_id, start_time_ms))";

    private static final String INSERT_SQL =
        "INSERT INTO battle_results " +
        "(room_id, start_time_ms, end_time_ms, total_frames, result, player_count, monster_count, cheat_score) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ROOM_SQL =
        "SELECT room_id, start_time_ms, end_time_ms, total_frames, result, player_count, monster_count, cheat_score " +
        "FROM battle_results WHERE room_id = ? ORDER BY start_time_ms DESC";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM battle_results";

    private final DataSource dataSource;

    public JdbcMatchResultRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfNotExists();
    }

    @Override
    public void save(MatchResult result) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, result.getRoomId());
            statement.setLong(2, result.getStartTimeMs());
            statement.setLong(3, result.getEndTimeMs());
            statement.setLong(4, result.getTotalFrames());
            statement.setInt(5, result.getResult());
            statement.setInt(6, result.getPlayerCount());
            statement.setInt(7, result.getMonsterCount());
            statement.setInt(8, result.getCheatScore());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to save match result", e);
        }
    }

    @Override
    public List<MatchResult> findByRoomId(String roomId) {
        List<MatchResult> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ROOM_SQL)) {
            statement.setString(1, roomId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to query match results", e);
        }
        return results;
    }

    @Override
    public long count() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_SQL);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to count match results", e);
        }
    }

    private void createTableIfNotExists() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL)) {
            statement.execute();
        } catch (SQLException e) {
            log.error("Failed to create battle_results table", e);
            throw new PersistenceException("Failed to create battle_results table", e);
        }
    }

    private MatchResult mapRow(ResultSet rs) throws SQLException {
        return new MatchResult(
            rs.getString("room_id"),
            rs.getLong("start_time_ms"),
            rs.getLong("end_time_ms"),
            rs.getLong("total_frames"),
            rs.getInt("result"),
            rs.getInt("player_count"),
            rs.getInt("monster_count"),
            rs.getInt("cheat_score")
        );
    }
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/StorageConfig.java`：

```java
package com.actiongame.server.persistence;

import java.util.Map;

public class StorageConfig {
    private final String matchDbUrl;
    private final String matchDbUser;
    private final String matchDbPassword;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;

    public StorageConfig(Map<String, String> env) {
        this.matchDbUrl = env.getOrDefault("MATCH_DB_URL", "");
        this.matchDbUser = env.getOrDefault("MATCH_DB_USER", "root");
        this.matchDbPassword = env.getOrDefault("MATCH_DB_PASSWORD", "");
        this.redisHost = env.getOrDefault("REDIS_HOST", "");
        this.redisPort = parsePort(env.getOrDefault("REDIS_PORT", "6379"));
        this.redisPassword = env.getOrDefault("REDIS_PASSWORD", "");
    }

    public static StorageConfig fromEnv() {
        return new StorageConfig(System.getenv());
    }

    public boolean isMySqlEnabled() {
        return !matchDbUrl.isEmpty();
    }

    public boolean isRedisEnabled() {
        return !redisHost.isEmpty();
    }

    public String getMatchDbUrl() { return matchDbUrl; }
    public String getMatchDbUser() { return matchDbUser; }
    public String getMatchDbPassword() { return matchDbPassword; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisPassword() { return redisPassword; }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 6379;
        }
    }
}
```

创建 `server/src/main/java/com/actiongame/server/persistence/StorageFactory.java`：

```java
package com.actiongame.server.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class StorageFactory {
    private static final int REDIS_TTL_SECONDS = 1800;

    private StorageFactory() {}

    public static MatchResultRepository createMatchResultRepository(StorageConfig config) {
        if (config.isMySqlEnabled()) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getMatchDbUrl());
            hikariConfig.setUsername(config.getMatchDbUser());
            hikariConfig.setPassword(config.getMatchDbPassword());
            hikariConfig.setMaximumPoolSize(10);
            return new JdbcMatchResultRepository(new HikariDataSource(hikariConfig));
        }
        return new InMemoryMatchResultRepository();
    }

    public static RoomStateCache createRoomStateCache(StorageConfig config) {
        if (config.isRedisEnabled()) {
            JedisPool pool;
            if (config.getRedisPassword().isEmpty()) {
                pool = new JedisPool(config.getRedisHost(), config.getRedisPort());
            } else {
                pool = new JedisPool(new JedisPoolConfig(), config.getRedisHost(),
                    config.getRedisPort(), 2000, config.getRedisPassword());
            }
            return new RedisRoomStateCache(new JedisStringStore(pool, REDIS_TTL_SECONDS));
        }
        return new InMemoryRoomStateCache();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -f server/pom.xml test -Dtest=JdbcMatchResultRepositoryTest,StorageConfigTest`

Expected: PASS，2 个测试全部通过。

- [ ] **Step 5: Commit**

```bash
git add server/src/main/java/com/actiongame/server/persistence server/src/test/java/com/actiongame/server/persistence
git commit -m "feat: add jdbc match repository and storage config"
```

## Task 5: 接入战斗结束与房间生命周期

**Files:**
- Modify: `server/src/main/java/com/actiongame/server/room/BattleRoom.java`
- Modify: `server/src/main/java/com/actiongame/server/room/RoomManager.java`
- Test: `server/src/test/java/com/actiongame/server/persistence/BattleRoomPersistenceTest.java`
- Test: `server/src/test/java/com/actiongame/server/persistence/RoomManagerPersistenceTest.java`

- [ ] **Step 1: 先写失败测试**

创建 `server/src/test/java/com/actiongame/server/persistence/BattleRoomPersistenceTest.java`：

```java
package com.actiongame.server.persistence;

import com.actiongame.server.config.MonsterConfig;
import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.PlayerCharacter;
import com.actiongame.server.room.BattleRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("战斗结束持久化")
class BattleRoomPersistenceTest {

    @Test
    @DisplayName("should_saveResultAndCacheState_when_battleEnds")
    void should_saveResultAndCacheState_when_battleEnds() {
        InMemoryMatchResultRepository repository = new InMemoryMatchResultRepository();
        InMemoryRoomStateCache cache = new InMemoryRoomStateCache();
        BattleRoom room = new BattleRoom("persist-room-1", repository, cache);

        PlayerCharacter player = new PlayerCharacter(0, 1,
            new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f), "persist-player");
        room.addPlayer(player, null);
        MonsterConfig config = new MonsterConfig();
        config.setMaxHealth(100f);
        room.spawnMonster(config, false);

        room.startBattle();
        room.endBattle();

        assertThat(repository.count()).isEqualTo(1);
        MatchResult saved = repository.findByRoomId("persist-room-1").get(0);
        assertThat(saved.getRoomId()).isEqualTo("persist-room-1");
        assertThat(saved.getPlayerCount()).isEqualTo(1);
        assertThat(saved.getMonsterCount()).isEqualTo(1);

        RoomState state = cache.get("persist-room-1");
        assertThat(state).isNotNull();
        assertThat(state.getStatus()).isEqualTo("SETTLEMENT");
    }
}
```

创建 `server/src/test/java/com/actiongame/server/persistence/RoomManagerPersistenceTest.java`：

```java
package com.actiongame.server.persistence;

import com.actiongame.server.room.BattleRoom;
import com.actiongame.server.room.RoomManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("房间生命周期持久化")
class RoomManagerPersistenceTest {

    @Test
    @DisplayName("should_cacheAndRemoveRoom_when_roomLifecycleManaged")
    void should_cacheAndRemoveRoom_when_roomLifecycleManaged() {
        InMemoryMatchResultRepository repository = new InMemoryMatchResultRepository();
        InMemoryRoomStateCache cache = new InMemoryRoomStateCache();
        RoomManager manager = new RoomManager(repository, cache);

        BattleRoom room = manager.createRoom("room-lifecycle-1");
        assertThat(cache.get("room-lifecycle-1")).isNotNull();

        manager.destroyRoom("room-lifecycle-1");
        assertThat(cache.get("room-lifecycle-1")).isNull();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -f server/pom.xml test -Dtest=BattleRoomPersistenceTest,RoomManagerPersistenceTest`

Expected: FAIL，原因是 `BattleRoom` 和 `RoomManager` 还没有接收仓库/缓存参数的构造器。

- [ ] **Step 3: 修改 BattleRoom**

在 `BattleRoom` 字段区新增：

```java
    private final MatchResultRepository matchResultRepository;
    private final RoomStateCache roomStateCache;
```

将现有构造器：

```java
    public BattleRoom(String roomId) {
        this.roomId = roomId;
        this.combatSystem = new CombatSystem();
        this.recycler = new ResourceRecycler();
        this.scheduler = new FrameScheduler(this);
        this.combatSystem.setBuffEngineResolver(entityId -> buffEngines.get(entityId));
        float dt = com.actiongame.server.constant.GameConstants.FRAME_INTERVAL_MS / 1000f;
        this.cheatDetector = new com.actiongame.server.anticheat.CheatDetector(dt);
        this.cheatResponseManager = new com.actiongame.server.anticheat.CheatResponseManager();
        this.llmService = new com.actiongame.server.llm.LocalLLMService();

        // 加载技能配置
        ConfigLoader configLoader = new ConfigLoader();
        List<SkillConfig> loaded = configLoader.loadListFromClasspath("config/skills.json", SkillConfig.class);
        this.skillConfigs = loaded.isEmpty() ? loadDefaultSkillConfigs() : loaded;
        log.info("Loaded {} skill configs for room {}", skillConfigs.size(), roomId);
    }
```

替换为：

```java
    public BattleRoom(String roomId) {
        this(roomId, new InMemoryMatchResultRepository(), new InMemoryRoomStateCache());
    }

    public BattleRoom(String roomId, MatchResultRepository matchResultRepository, RoomStateCache roomStateCache) {
        this.roomId = roomId;
        this.matchResultRepository = matchResultRepository;
        this.roomStateCache = roomStateCache;
        this.combatSystem = new CombatSystem();
        this.recycler = new ResourceRecycler();
        this.scheduler = new FrameScheduler(this);
        this.combatSystem.setBuffEngineResolver(entityId -> buffEngines.get(entityId));
        float dt = com.actiongame.server.constant.GameConstants.FRAME_INTERVAL_MS / 1000f;
        this.cheatDetector = new com.actiongame.server.anticheat.CheatDetector(dt);
        this.cheatResponseManager = new com.actiongame.server.anticheat.CheatResponseManager();
        this.llmService = new com.actiongame.server.llm.LocalLLMService();

        // 加载技能配置
        ConfigLoader configLoader = new ConfigLoader();
        List<SkillConfig> loaded = configLoader.loadListFromClasspath("config/skills.json", SkillConfig.class);
        this.skillConfigs = loaded.isEmpty() ? loadDefaultSkillConfigs() : loaded;
        log.info("Loaded {} skill configs for room {}", skillConfigs.size(), roomId);
    }
```

在 `endBattle()` 方法末尾（LLM 分析 try/catch 之后）新增调用：

```java
        persistBattleResult();
```

在 `endBattle()` 方法之后新增方法：

```java
    private void persistBattleResult() {
        try {
            int result = monsters.isEmpty() ? 1 : 2;
            MatchResult matchResult = new MatchResult(
                roomId,
                startTimeMs,
                System.currentTimeMillis(),
                currentFrameIndex,
                result,
                players.size(),
                monsters.size(),
                cheatDetector.getTotalViolationScore()
            );
            matchResultRepository.save(matchResult);

            RoomState state = new RoomState(
                roomId,
                status.get().name(),
                players.size(),
                monsters.size(),
                currentFrameIndex,
                System.currentTimeMillis()
            );
            roomStateCache.put(roomId, state);
            log.info("Match result persisted: room={} result={} frames={}", roomId, result, currentFrameIndex);
        } catch (Exception e) {
            log.warn("Failed to persist match result (non-fatal): {}", e.getMessage());
        }
    }
```

在文件顶部 import 区新增：

```java
import com.actiongame.server.persistence.InMemoryMatchResultRepository;
import com.actiongame.server.persistence.InMemoryRoomStateCache;
import com.actiongame.server.persistence.MatchResult;
import com.actiongame.server.persistence.MatchResultRepository;
import com.actiongame.server.persistence.RoomState;
import com.actiongame.server.persistence.RoomStateCache;
```

- [ ] **Step 4: 修改 RoomManager**

在 `RoomManager` 字段区新增：

```java
    private final MatchResultRepository matchResultRepository;
    private final RoomStateCache roomStateCache;
```

将单例与构造器：

```java
    public static synchronized RoomManager getInstance() {
        if (instance == null) {
            instance = new RoomManager();
        }
        return instance;
    }
```

替换为：

```java
    public static synchronized RoomManager getInstance() {
        if (instance == null) {
            StorageConfig config = StorageConfig.fromEnv();
            instance = new RoomManager(
                StorageFactory.createMatchResultRepository(config),
                StorageFactory.createRoomStateCache(config)
            );
        }
        return instance;
    }

    public RoomManager() {
        this(new InMemoryMatchResultRepository(), new InMemoryRoomStateCache());
    }

    public RoomManager(MatchResultRepository matchResultRepository, RoomStateCache roomStateCache) {
        this.matchResultRepository = matchResultRepository;
        this.roomStateCache = roomStateCache;
    }
```

将 `createRoom`：

```java
    public BattleRoom createRoom(String roomId) {
        BattleRoom room = new BattleRoom(roomId);
        rooms.put(roomId, room);
        log.info("Room created: {}", roomId);
        return room;
    }
```

替换为：

```java
    public BattleRoom createRoom(String roomId) {
        BattleRoom room = new BattleRoom(roomId, matchResultRepository, roomStateCache);
        rooms.put(roomId, room);
        roomStateCache.put(roomId, toRoomState(room));
        log.info("Room created: {}", roomId);
        return room;
    }
```

将 `getOrCreateRoom`：

```java
    public BattleRoom getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, BattleRoom::new);
    }
```

替换为：

```java
    public BattleRoom getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, id -> {
            BattleRoom room = new BattleRoom(id, matchResultRepository, roomStateCache);
            roomStateCache.put(id, toRoomState(room));
            return room;
        });
    }
```

将 `destroyRoom`：

```java
    public void destroyRoom(String roomId) {
        BattleRoom room = rooms.remove(roomId);
        if (room != null) {
            room.endBattle();
            log.info("Room destroyed: {}", roomId);
        }
    }
```

替换为：

```java
    public void destroyRoom(String roomId) {
        BattleRoom room = rooms.remove(roomId);
        if (room != null) {
            room.endBattle();
            roomStateCache.remove(roomId);
            log.info("Room destroyed: {}", roomId);
        }
    }
```

在 `cleanupExpiredRooms()` 中，移除房间后新增缓存删除：

```java
                roomStateCache.remove(entry.getKey());
```

在文件末尾新增私有方法：

```java
    private RoomState toRoomState(BattleRoom room) {
        return new RoomState(
            room.getRoomId(),
            room.getStatus().name(),
            room.getPlayerCount(),
            room.getMonsters().size(),
            room.getCurrentFrameIndex(),
            System.currentTimeMillis()
        );
    }
```

在文件顶部 import 区新增：

```java
import com.actiongame.server.persistence.InMemoryMatchResultRepository;
import com.actiongame.server.persistence.InMemoryRoomStateCache;
import com.actiongame.server.persistence.MatchResultRepository;
import com.actiongame.server.persistence.RoomState;
import com.actiongame.server.persistence.RoomStateCache;
import com.actiongame.server.persistence.StorageConfig;
import com.actiongame.server.persistence.StorageFactory;
```

- [ ] **Step 5: 运行新测试确认通过**

Run: `mvn -q -f server/pom.xml test -Dtest=BattleRoomPersistenceTest,RoomManagerPersistenceTest`

Expected: PASS，2 个测试全部通过。

- [ ] **Step 6: 运行既有房间测试确认无回归**

Run: `mvn -q -f server/pom.xml test -Dtest=BattleRoomTest,RoomManagerTest,EndToEndBattleFlowTest`

Expected: PASS，全部通过。

- [ ] **Step 7: Commit**

```bash
git add server/src/main/java/com/actiongame/server/room server/src/test/java/com/actiongame/server/persistence
git commit -m "feat: persist match results and room state"
```

## Task 6: Docker Compose 与环境变量

**Files:**
- Modify: `docker-compose.yml`
- Modify: `.env.example`

- [ ] **Step 1: 在 `docker-compose.yml` 新增 MySQL 与 Redis 服务**

将 `services` 段整体替换为：

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: actiongame-mysql
    environment:
      MYSQL_ROOT_PASSWORD: actiongame
      MYSQL_DATABASE: actiongame
    ports:
      - "${MYSQL_PORT:-3306}:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-pactiongame"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: actiongame-redis
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  game-server:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: actiongame-server
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    ports:
      - "${SERVER_PORT:-9090}:9090"
    environment:
      - SERVER_PORT=9090
      - JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC
      - MATCH_DB_URL=jdbc:mysql://mysql:3306/actiongame?useSSL=false&serverTimezone=UTC
      - MATCH_DB_USER=root
      - MATCH_DB_PASSWORD=actiongame
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    volumes:
      - server-logs:/app/logs
      - server-replays:/app/replays
      - ./config:/app/config:ro
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-sf", "http://localhost:9090/game"]
      interval: 30s
      timeout: 5s
      start_period: 10s
      retries: 3
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
```

将 `volumes` 段替换为：

```yaml
volumes:
  server-logs:
    driver: local
  server-replays:
    driver: local
  mysql-data:
    driver: local
  redis-data:
    driver: local
```

- [ ] **Step 2: 更新 `.env.example`**

将末尾 `Future: Database` 段落替换为：

```dotenv
# ===== Persistence =====
MATCH_DB_URL=jdbc:mysql://localhost:3306/actiongame?useSSL=false&serverTimezone=UTC
MATCH_DB_USER=root
MATCH_DB_PASSWORD=actiongame
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# ===== Docker ports =====
MYSQL_PORT=3306
```

- [ ] **Step 3: 验证 Compose 配置**

Run: `docker compose config -q`

Expected: 退出码 0，无格式错误。

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml .env.example
git commit -m "chore: add mysql and redis to docker compose"
```

## Task 7: 全量验证

**Files:**
- 无新增，只读验证

- [ ] **Step 1: 运行全部服务端测试**

Run: `mvn -q -f server/pom.xml test`

Expected: BUILD SUCCESS，全部测试通过。

- [ ] **Step 2: 确认 Compose 仍可解析**

Run: `docker compose config -q`

Expected: 退出码 0。

- [ ] **Step 3: 检查改动范围**

Run: `git status --short -- server docker-compose.yml .env.example`

Expected: 只显示本次 Phase 2 修改/新增的文件，不包含 `Assets/`。
