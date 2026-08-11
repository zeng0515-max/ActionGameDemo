package com.actiongame.server.room;

import com.actiongame.server.ai.decision.BossAIController;
import com.actiongame.server.anticheat.AntiCheatConfig;
import com.actiongame.server.battle.buffengine.BuffEngine;
import com.actiongame.server.battle.combatsystem.CombatSystem;
import com.actiongame.server.config.ConfigLoader;
import com.actiongame.server.config.MonsterConfig;
import com.actiongame.server.config.SkillConfig;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.domain.character.CharacterState;
import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.MonsterCharacter;
import com.actiongame.server.domain.character.PlayerCharacter;
import com.actiongame.server.domain.buff.AttributeType;
import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.buff.BuffStackingRule;
import com.actiongame.server.domain.buff.BuffType;
import com.actiongame.server.domain.combat.ElementType;
import com.actiongame.server.domain.combat.HitResult;
import com.actiongame.server.net.session.GameSession;
import com.actiongame.server.persistence.InMemoryMatchResultRepository;
import com.actiongame.server.persistence.InMemoryRoomStateCache;
import com.actiongame.server.persistence.MatchResult;
import com.actiongame.server.persistence.MatchResultRepository;
import com.actiongame.server.persistence.RoomState;
import com.actiongame.server.persistence.RoomStateCache;
import com.actiongame.server.util.Quaternion;
import com.actiongame.server.util.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 战斗房间 (对应文档3.5 BattleRoom)
 * 核心调度单元: 管理玩家、怪物、帧循环、战斗结算
 * 实现 FrameExecutor: 每帧执行 AI→移动→战斗→Buff→快照
 */
public class BattleRoom implements FrameExecutor {
    private static final Logger log = LoggerFactory.getLogger(BattleRoom.class);

    private final String roomId;
    private final AtomicReference<RoomStatus> status = new AtomicReference<>(RoomStatus.WAITING);
    private final List<RoomPlayer> players = new CopyOnWriteArrayList<>();
    private final List<RoomMonster> monsters = new CopyOnWriteArrayList<>();
    private final Map<Integer, BuffEngine> buffEngines = new ConcurrentHashMap<>();
    private final Map<Integer, Character> entityMap = new ConcurrentHashMap<>();
    private final Map<Integer, RoomPlayer> playerByEntityId = new ConcurrentHashMap<>();

    private final CombatSystem combatSystem;
    private final ResourceRecycler recycler;
    private final FrameScheduler scheduler;
    private final MatchResultRepository matchResultRepository;
    private final RoomStateCache roomStateCache;

    /** 技能配置: index 0=普攻, 1=技能, 2=终极技 */
    private final List<SkillConfig> skillConfigs;

    /** 反作弊检测器 */
    private final com.actiongame.server.anticheat.CheatDetector cheatDetector;
    /** 作弊响应管理器 */
    private final com.actiongame.server.anticheat.CheatResponseManager cheatResponseManager;

    /** LLM 服务 */
    private final com.actiongame.server.llm.LLMService llmService;

    private final AtomicInteger entityIdGenerator = new AtomicInteger(1);
    private volatile long currentFrameIndex = 0;
    private volatile long startTimeMs = 0;
    private volatile boolean migrationHold = false;

    private final List<PendingAction> pendingActions = new ArrayList<>();
    private final ReentrantLock pendingActionsLock = new ReentrantLock();

    private static final int MAX_PLAYERS = 4;
    private static final int SNAPSHOT_INTERVAL_FRAMES = 100;

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

    private List<SkillConfig> loadDefaultSkillConfigs() {
        List<SkillConfig> defaults = new ArrayList<>();
        SkillConfig normal = new SkillConfig();
        normal.setSkillName("Normal Attack");
        normal.setDamageMultiplier(1.0f);
        normal.setCooldown(0.5f);
        normal.setRange(2.5f);
        defaults.add(normal);
        SkillConfig skill = new SkillConfig();
        skill.setSkillName("Heavy Strike");
        skill.setDamageMultiplier(2.5f);
        skill.setCooldown(8.0f);
        skill.setRange(3.0f);
        defaults.add(skill);
        SkillConfig ult = new SkillConfig();
        ult.setSkillName("Ultimate Nova");
        ult.setDamageMultiplier(5.0f);
        ult.setCooldown(30.0f);
        ult.setRange(8.0f);
        defaults.add(ult);
        return defaults;
    }

    // === 玩家管理 ===

    public int addPlayer(PlayerCharacter character, GameSession session) {
        // 清理已断线玩家 (回收器只在帧循环里跑, 房间未开战时需要手动清理)
        if (!migrationHold) {
            recycler.recycleOfflinePlayers(players, this::removePlayer);
        }

        // 同一玩家重连: 先移除旧记录
        String newPlayerId = character.getPlayerId();
        players.removeIf(p -> {
            if (p.getPlayerId().equals(newPlayerId)) {
                buffEngines.remove(p.getEntityId());
                entityMap.remove(p.getEntityId());
                playerByEntityId.remove(p.getEntityId());
                log.info("Removed stale player {} from room {}", newPlayerId, roomId);
                return true;
            }
            return false;
        });

        if (players.size() >= MAX_PLAYERS) {
            log.warn("Room {} is full ({} players), rejected playerId={}",
                roomId, MAX_PLAYERS, character.getPlayerId());
            return -1;
        }
        int entityId = entityIdGenerator.getAndIncrement();
        character.setEntityId(entityId);
        RoomPlayer player = new RoomPlayer(entityId, character, session);
        players.add(player);
        playerByEntityId.put(entityId, player);
        buffEngines.put(entityId, new BuffEngine(character));
        entityMap.put(entityId, character);
        log.info("Player {} joined room {}, entityId={}", character.getPlayerId(), roomId, entityId);
        return entityId;
    }

    public void removePlayer(int entityId) {
        players.removeIf(p -> p.getEntityId() == entityId);
        buffEngines.remove(entityId);
        entityMap.remove(entityId);
        playerByEntityId.remove(entityId);
        log.info("Player entityId={} left room {}", entityId, roomId);
    }

    // === 怪物管理 ===

    public int spawnMonster(MonsterConfig config, boolean isBoss) {
        int entityId = entityIdGenerator.getAndIncrement();
        CharacterStats stats = new CharacterStats(
            config.getMaxHealth(), config.getAttackPower(), config.getDefense(),
            config.getMoveSpeed(), 0.05f, 1.5f
        );
        MonsterCharacter monster = new MonsterCharacter(entityId, 1, stats);
        RoomMonster rm = new RoomMonster(monster, config, isBoss);
        // 设置初始位置 (分散在场景中)
        float monsterCount = monsters.size();
        float angle = monsterCount * 2.0f; // ~115度间隔
        float radius = isBoss ? 0f : 5f;
        monster.setPosition(new Vector3(
            (float) Math.cos(angle) * radius, 0, (float) Math.sin(angle) * radius + 5f
        ));
        rm.initialize();
        monsters.add(rm);
        buffEngines.put(entityId, new BuffEngine(monster));
        entityMap.put(entityId, monster);
        log.info("Monster spawned in room {}, entityId={}, boss={}, pos=({},{},{})",
            roomId, entityId, isBoss, monster.getPosition().x, monster.getPosition().y, monster.getPosition().z);
        return entityId;
    }

    // === 战斗控制 ===

    public void startBattle() {
        if (!status.compareAndSet(RoomStatus.WAITING, RoomStatus.BATTLE)) return;
        startTimeMs = System.currentTimeMillis();
        scheduler.start();
        log.info("Battle started in room {}", roomId);
    }

    public void endBattle() {
        if (!status.compareAndSet(RoomStatus.BATTLE, RoomStatus.SETTLEMENT)) {
            if (status.get() == RoomStatus.SETTLEMENT || status.get() == RoomStatus.CLOSED) return;
            status.set(RoomStatus.SETTLEMENT);
        }
        scheduler.stop();

        // Layer 3: 回放分析 (如果有录制器)
        // 注: BattleRecorder 由上层注入, 此处仅触发检测器已有的数据分析
        int totalScore = cheatDetector.getTotalViolationScore();
        if (totalScore > 0) {
            log.warn("[AntiCheat] Room {} battle ended with total violation score={}",
                roomId, totalScore);
            for (var incident : cheatDetector.getIncidents()) {
                cheatResponseManager.submitIncident(incident);
            }
        }

        log.info("Battle ended in room {}, total frames={}", roomId, currentFrameIndex);

        // Phase 10: LLM 分析
        try {
            var ctx = new com.actiongame.server.llm.BattleContext(roomId, currentFrameIndex,
                System.currentTimeMillis() - startTimeMs);
            ctx.setPlayerCount(players.size());
            ctx.setMonsterCount(monsters.size());
            ctx.setCheatIncidents(cheatDetector.getIncidents());
            ctx.setBattleResult(monsters.isEmpty() ? 1 : 2);

            // 战斗总结
            String summary = llmService.generateBattleSummary(ctx);
            log.info("[LLM] Battle summary: {}", summary);

            // 难度分析
            var suggestion = llmService.analyzeDifficulty(ctx);
            log.info("[LLM] Difficulty: {}", suggestion);

            // 作弊分析 (如果有作弊事件)
            if (!cheatDetector.getIncidents().isEmpty()) {
                var analysis = llmService.analyzeCheatPatterns(ctx);
                log.info("[LLM] Cheat analysis: {}", analysis);
            }
        } catch (Exception e) {
            log.warn("[LLM] Analysis failed (non-fatal): {}", e.getMessage());
        }

        persistBattleResult();
    }

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

    public void closeRoom() {
        endBattle();
        status.set(RoomStatus.CLOSED);
    }

    // === 房间迁移 ===

    public RoomSnapshot toSnapshot() {
        List<RoomSnapshot.PlayerSnapshot> playerSnapshots = new ArrayList<>();
        for (RoomPlayer player : players) {
            playerSnapshots.add(toPlayerSnapshot((PlayerCharacter) player.getCharacter()));
        }
        List<RoomSnapshot.MonsterSnapshot> monsterSnapshots = new ArrayList<>();
        for (RoomMonster monster : monsters) {
            monsterSnapshots.add(toMonsterSnapshot(monster));
        }
        return new RoomSnapshot(
            roomId,
            status.get().name(),
            currentFrameIndex,
            startTimeMs,
            entityIdGenerator.get(),
            playerSnapshots,
            monsterSnapshots
        );
    }

    public void restoreFromSnapshot(RoomSnapshot snapshot) {
        currentFrameIndex = snapshot.currentFrameIndex();
        startTimeMs = snapshot.startTimeMs();
        entityIdGenerator.set(Math.max(1, snapshot.nextEntityId()));

        for (RoomSnapshot.PlayerSnapshot player : snapshot.players()) {
            restorePlayer(player);
        }
        for (RoomSnapshot.MonsterSnapshot monster : snapshot.monsters()) {
            restoreMonster(monster);
        }

        status.set(RoomStatus.valueOf(snapshot.status()));
        migrationHold = true;
        log.info("Room {} restored from snapshot, frame={}, players={}, monsters={}",
            roomId, currentFrameIndex, players.size(), monsters.size());
    }

    public void suspendForMigration() {
        scheduler.stop();
        log.info("Room {} suspended for migration", roomId);
    }

    public void resumeBattle() {
        if (status.get() == RoomStatus.BATTLE) {
            scheduler.start();
        }
    }

    private RoomSnapshot.PlayerSnapshot toPlayerSnapshot(PlayerCharacter character) {
        return new RoomSnapshot.PlayerSnapshot(
            character.getEntityId(),
            character.getPlayerId(),
            character.getConfigId(),
            character.getLevel(),
            character.getCurrentExp(),
            character.getAvailableSkillPoints(),
            character.getState().name(),
            character.getPosition().x,
            character.getPosition().y,
            character.getPosition().z,
            character.getRotation().x,
            character.getRotation().y,
            character.getRotation().z,
            character.getRotation().w,
            character.getElementType().name(),
            character.getShieldAmount(),
            character.isInvincible(),
            character.isDead(),
            character.getTargetEntityId(),
            character.getComboStep(),
            toStatsSnapshot(character.getStats()),
            toBuffSnapshots(activeBuffsOf(character.getEntityId()))
        );
    }

    private RoomSnapshot.MonsterSnapshot toMonsterSnapshot(RoomMonster roomMonster) {
        MonsterCharacter character = roomMonster.getCharacter();
        return new RoomSnapshot.MonsterSnapshot(
            character.getEntityId(),
            character.getConfigId(),
            roomMonster.getAiController() instanceof com.actiongame.server.ai.decision.BossAIController,
            character.getState().name(),
            character.getPosition().x,
            character.getPosition().y,
            character.getPosition().z,
            character.getRotation().x,
            character.getRotation().y,
            character.getRotation().z,
            character.getRotation().w,
            character.getElementType().name(),
            character.getShieldAmount(),
            character.isInvincible(),
            character.isDead(),
            character.getTargetEntityId(),
            character.getComboStep(),
            character.getMonsterType(),
            character.getDetectionRange(),
            character.getAttackRange(),
            character.getAttackCooldown(),
            character.getPatrolRadius(),
            character.getFleeThreshold(),
            roomMonster.getAiController().getConfig().getEnemyName(),
            toStatsSnapshot(character.getStats()),
            toBuffSnapshots(activeBuffsOf(character.getEntityId()))
        );
    }

    private List<Buff> activeBuffsOf(int entityId) {
        BuffEngine engine = buffEngines.get(entityId);
        return engine == null ? List.of() : engine.getActiveBuffs();
    }

    private RoomSnapshot.StatsSnapshot toStatsSnapshot(CharacterStats stats) {
        return new RoomSnapshot.StatsSnapshot(
            stats.getMaxHealth(),
            stats.getCurrentHealth(),
            stats.getAttackPower(),
            stats.getDefense(),
            stats.getMoveSpeed(),
            stats.getCriticalRate(),
            stats.getCriticalDamageMultiplier(),
            stats.getMaxEnergy(),
            stats.getCurrentEnergy()
        );
    }

    private List<RoomSnapshot.BuffSnapshot> toBuffSnapshots(List<Buff> buffs) {
        List<RoomSnapshot.BuffSnapshot> snapshots = new ArrayList<>();
        for (Buff buff : buffs) {
            snapshots.add(new RoomSnapshot.BuffSnapshot(
                buff.getBuffId(),
                buff.getBuffName(),
                buff.getBuffType().name(),
                buff.getDuration(),
                buff.getRemainingTime(),
                buff.getStacks(),
                buff.getMaxStacks(),
                buff.getStackingRule().name(),
                buff.getTickInterval(),
                buff.getTickValuePercent(),
                buff.getAttributeType().name(),
                buff.getAttributeModifier(),
                buff.getShieldValue(),
                buff.getRelatedElement().name(),
                buff.getTargetEntityId(),
                buff.getSourceEntityId(),
                buff.isActive()
            ));
        }
        return snapshots;
    }

    private void restoreBuffs(Character character, List<RoomSnapshot.BuffSnapshot> buffSnapshots) {
        if (buffSnapshots == null) return;
        BuffEngine engine = buffEngines.get(character.getEntityId());
        if (engine == null) return;

        for (RoomSnapshot.BuffSnapshot snapshot : buffSnapshots) {
            if (!snapshot.active()) continue;
            Buff buff = new Buff(
                snapshot.buffId(),
                snapshot.buffName(),
                parseBuffType(snapshot.buffType()),
                snapshot.duration(),
                snapshot.maxStacks(),
                parseStackingRule(snapshot.stackingRule()),
                snapshot.tickInterval(),
                snapshot.tickValuePercent(),
                parseAttributeType(snapshot.attributeType()),
                snapshot.attributeModifier(),
                snapshot.shieldValue(),
                parseElement(snapshot.relatedElement())
            );
            buff.setRemainingTime(snapshot.remainingTime());
            buff.setStacks(snapshot.stacks());
            buff.setTargetEntityId(snapshot.targetEntityId());
            buff.setSourceEntityId(snapshot.sourceEntityId());
            buff.setActive(true);

            Character source = entityMap.get(snapshot.sourceEntityId());
            engine.restoreBuff(buff, source);
        }
    }

    private static BuffType parseBuffType(String value) {
        try {
            return BuffType.valueOf(value);
        } catch (Exception e) {
            return BuffType.ATTRIBUTE;
        }
    }

    private static BuffStackingRule parseStackingRule(String value) {
        try {
            return BuffStackingRule.valueOf(value);
        } catch (Exception e) {
            return BuffStackingRule.REFRESH_DURATION;
        }
    }

    private static AttributeType parseAttributeType(String value) {
        try {
            return AttributeType.valueOf(value);
        } catch (Exception e) {
            return AttributeType.ATTACK_POWER;
        }
    }

    private void restorePlayer(RoomSnapshot.PlayerSnapshot snapshot) {
        CharacterStats stats = restoreStats(snapshot.stats());
        PlayerCharacter character = new PlayerCharacter(
            snapshot.entityId(), snapshot.configId(), stats, snapshot.playerId());
        character.setLevel(snapshot.level());
        character.setCurrentExp(snapshot.currentExp());
        character.setAvailableSkillPoints(snapshot.availableSkillPoints());
        applyCharacterSnapshot(character, snapshot);

        RoomPlayer player = new RoomPlayer(snapshot.entityId(), character, null);
        players.add(player);
        playerByEntityId.put(snapshot.entityId(), player);
        entityMap.put(snapshot.entityId(), character);
        buffEngines.put(snapshot.entityId(), new BuffEngine(character));
        restoreBuffs(character, snapshot.buffs());
    }

    private void restoreMonster(RoomSnapshot.MonsterSnapshot snapshot) {
        CharacterStats stats = restoreStats(snapshot.stats());
        MonsterCharacter monster = new MonsterCharacter(snapshot.entityId(), snapshot.configId(), stats);
        monster.setMonsterType(snapshot.monsterType());
        monster.setDetectionRange(snapshot.detectionRange());
        monster.setAttackRange(snapshot.attackRange());
        monster.setAttackCooldown(snapshot.attackCooldown());
        monster.setPatrolRadius(snapshot.patrolRadius());
        monster.setFleeThreshold(snapshot.fleeThreshold());
        applyCharacterSnapshot(monster, snapshot);

        MonsterConfig config = new MonsterConfig();
        config.setEnemyName(snapshot.enemyName());
        config.setMaxHealth(stats.getMaxHealth());
        config.setAttackPower(stats.getAttackPower());
        config.setDefense(stats.getDefense());
        config.setMoveSpeed(stats.getMoveSpeed());
        config.setDetectionRange(snapshot.detectionRange());
        config.setAttackRange(snapshot.attackRange());
        config.setAttackCooldown(snapshot.attackCooldown());
        config.setPatrolRadius(snapshot.patrolRadius());
        config.setFleeThreshold(snapshot.fleeThreshold());

        RoomMonster roomMonster = new RoomMonster(monster, config, snapshot.boss());
        roomMonster.initialize();
        monsters.add(roomMonster);
        entityMap.put(snapshot.entityId(), monster);
        buffEngines.put(snapshot.entityId(), new BuffEngine(monster));
        restoreBuffs(monster, snapshot.buffs());
    }

    private void applyCharacterSnapshot(Character character, RoomSnapshot.PlayerSnapshot snapshot) {
        character.setDead(snapshot.dead());
        character.setState(parseState(snapshot.state()));
        character.setPosition(new Vector3(snapshot.posX(), snapshot.posY(), snapshot.posZ()));
        character.setRotation(new Quaternion(
            snapshot.rotX(), snapshot.rotY(), snapshot.rotZ(), snapshot.rotW()));
        character.setElementType(parseElement(snapshot.element()));
        character.setInvincible(snapshot.invincible());
        character.setTargetEntityId(snapshot.targetEntityId());
        character.setComboStep(snapshot.comboStep());
    }

    private void applyCharacterSnapshot(Character character, RoomSnapshot.MonsterSnapshot snapshot) {
        character.setDead(snapshot.dead());
        character.setState(parseState(snapshot.state()));
        character.setPosition(new Vector3(snapshot.posX(), snapshot.posY(), snapshot.posZ()));
        character.setRotation(new Quaternion(
            snapshot.rotX(), snapshot.rotY(), snapshot.rotZ(), snapshot.rotW()));
        character.setElementType(parseElement(snapshot.element()));
        character.setInvincible(snapshot.invincible());
        character.setTargetEntityId(snapshot.targetEntityId());
        character.setComboStep(snapshot.comboStep());
    }

    private CharacterStats restoreStats(RoomSnapshot.StatsSnapshot snapshot) {
        CharacterStats stats = new CharacterStats(
            snapshot.maxHealth(),
            snapshot.attackPower(),
            snapshot.defense(),
            snapshot.moveSpeed(),
            snapshot.criticalRate(),
            snapshot.criticalDamageMultiplier());
        stats.setMaxEnergy(snapshot.maxEnergy());
        stats.setCurrentEnergy(snapshot.currentEnergy());
        stats.setCurrentHealth(snapshot.currentHealth());
        return stats;
    }

    private static CharacterState parseState(String state) {
        try {
            return CharacterState.valueOf(state);
        } catch (Exception e) {
            return CharacterState.IDLE;
        }
    }

    private static ElementType parseElement(String element) {
        try {
            return ElementType.valueOf(element);
        } catch (Exception e) {
            return ElementType.NONE;
        }
    }

    // === 玩家操作 ===

    public void submitPlayerAction(int entityId, int actionType, float moveX, float moveZ,
                                    int skillId, int targetEntityId) {
        // Layer 1: 实时反作弊检测
        long timestamp = System.currentTimeMillis();
        RoomPlayer player = findPlayer(entityId);
        float maxMoveSpeed = player != null
            ? player.getCharacter().getStats().getEffectiveMoveSpeed()
            : AntiCheatConfig.MAX_MOVE_SPEED;
        boolean detected = cheatDetector.checkRealtime(entityId, actionType, moveX, moveZ, timestamp, maxMoveSpeed);
        if (detected) {
            log.warn("[AntiCheat] Player entityId={} action rejected (cheat detected)", entityId);
            return; // 拒绝操作
        }

        pendingActionsLock.lock();
        try {
            pendingActions.add(new PendingAction(entityId, actionType, moveX, moveZ, skillId, targetEntityId));
        } finally {
            pendingActionsLock.unlock();
        }
    }

    // === 帧循环 (FrameExecutor) ===

    @Override
    public void executeFrame(long frameIndex, float deltaTime) {
        currentFrameIndex = frameIndex;

        // 1. 处理玩家操作
        processPendingActions(deltaTime);

        // 2. AI更新
        for (RoomMonster rm : monsters) {
            try {
                rm.update(deltaTime);
            } catch (Exception e) {
                log.error("Error updating monster AI entityId={}", rm.getEntityId(), e);
            }
        }

        // 3. Buff更新
        for (RoomPlayer rp : players) {
            BuffEngine engine = buffEngines.get(rp.getEntityId());
            if (engine != null) {
                try { engine.update(deltaTime); } catch (Exception e) {
                    log.error("Error updating player buffs entityId={}", rp.getEntityId(), e);
                }
            }
        }
        for (RoomMonster rm : monsters) {
            BuffEngine engine = buffEngines.get(rm.getEntityId());
            if (engine != null) {
                try { engine.update(deltaTime); } catch (Exception e) {
                    log.error("Error updating monster buffs entityId={}", rm.getEntityId(), e);
                }
            }
        }

        // 4. 资源回收
        recycler.recycleDeadMonsters(monsters);
        updateMigrationHold();
        if (!migrationHold) {
            recycler.recycleOfflinePlayers(players, this::removePlayer);
        }

        // 5. 更新反作弊追踪 (玩家位置)
        for (RoomPlayer rp : players) {
            cheatDetector.updatePlayerPosition(rp.getEntityId(), rp.getCharacter().getPosition());
        }

        // 6. 生成帧快照
        BattleFrame frame = buildFrameSnapshot(frameIndex);

        // 7. 广播帧快照
        broadcastFrame(frame);

        // 8. 检查战斗结束
        checkBattleEnd();

        // 9. 周期性写入迁移快照
        persistSnapshotIfNeeded();
    }

    private void persistSnapshotIfNeeded() {
        if (currentFrameIndex % SNAPSHOT_INTERVAL_FRAMES != 0) return;
        try {
            roomStateCache.putSnapshot(roomId, toSnapshot());
        } catch (Exception e) {
            log.warn("Failed to persist room snapshot for {}: {}", roomId, e.getMessage());
        }
    }

    private void processPendingActions(float deltaTime) {
        List<PendingAction> batch = new ArrayList<>();
        pendingActionsLock.lock();
        try {
            if (!pendingActions.isEmpty()) {
                batch.addAll(pendingActions);
                pendingActions.clear();
            }
        } finally {
            pendingActionsLock.unlock();
        }

        for (PendingAction action : batch) {
            RoomPlayer player = findPlayer(action.entityId());
            if (player == null || player.getCharacter().isDead()) continue;

            PlayerCharacter pc = player.getCharacter();
            switch (action.actionType()) {
                case 1 -> { // MOVE
                    float speed = pc.getStats().getEffectiveMoveSpeed();
                    pc.setPosition(pc.getPosition().add(
                        new Vector3(action.moveX() * speed * deltaTime, 0, action.moveZ() * speed * deltaTime)
                    ));
                }
                case 2 -> pc.setState(com.actiongame.server.domain.character.CharacterState.JUMP);
                case 3 -> executePlayerAttack(pc, 0);   // Normal Attack
                case 4 -> executePlayerAttack(pc, 1);   // Skill (Heavy Strike)
                case 5 -> executePlayerAttack(pc, 2);   // Ultimate (Nova)
                case 6 -> pc.setState(com.actiongame.server.domain.character.CharacterState.DODGE);
            }
        }
    }

    private void updateMigrationHold() {
        if (!migrationHold) return;
        boolean allPlayersActive = players.stream()
            .allMatch(player -> player.getSession() != null && player.getSession().isActive());
        if (allPlayersActive) {
            migrationHold = false;
        }
    }

    private void executePlayerAttack(PlayerCharacter attacker, int skillIndex) {
        if (skillIndex < 0 || skillIndex >= skillConfigs.size()) return;
        SkillConfig skill = skillConfigs.get(skillIndex);

        com.actiongame.server.domain.character.CharacterState state = switch (skillIndex) {
            case 0 -> com.actiongame.server.domain.character.CharacterState.ATTACK;
            case 1 -> com.actiongame.server.domain.character.CharacterState.SKILL;
            case 2 -> com.actiongame.server.domain.character.CharacterState.ULTIMATE;
            default -> com.actiongame.server.domain.character.CharacterState.ATTACK;
        };
        attacker.setState(state);

        float attackRange = skill.getRange();
        float skillMultiplier = skill.getDamageMultiplier();

        List<Character> candidates = new ArrayList<>();
        for (RoomMonster rm : monsters) {
            if (!rm.getCharacter().isDead()) candidates.add(rm.getCharacter());
        }

        List<HitResult> results = combatSystem.executeAreaAttack(
            attacker, attacker.getPosition(), attackRange, candidates,
            new HashSet<>(), skillMultiplier, ElementType.NONE
        );

        for (HitResult result : results) {
            if (result.isHit()) {
                // Layer 2: 伤害逻辑校验 (传入实际目标)
                Character target = entityMap.get(result.getTargetEntityId());
                boolean cheatDetected = cheatDetector.checkLogic(attacker, target, result.getDamage());
                if (cheatDetected) {
                    log.warn("[AntiCheat] Damage anomaly: attacker={} target={} damage={}",
                        attacker.getEntityId(), result.getTargetEntityId(), result.getDamage());
                }
                log.debug("Player {} hit target {} for {} damage (skill={}, mult={})",
                    attacker.getEntityId(), result.getTargetEntityId(), result.getDamage(),
                    skill.getSkillName(), skillMultiplier);
            }
        }
    }

    private BattleFrame buildFrameSnapshot(long frameIndex) {
        BattleFrame frame = new BattleFrame(frameIndex, System.currentTimeMillis(), roomId);
        for (RoomPlayer rp : players) {
            frame.addCharacterSnapshot(rp.getCharacter(), 0);
        }
        for (RoomMonster rm : monsters) {
            int type = rm.getAiController() instanceof BossAIController ? 2 : 1;
            frame.addCharacterSnapshot(rm.getCharacter(), type);
        }
        return frame;
    }

    private void broadcastFrame(BattleFrame frame) {
        byte[] payload = null;
        for (RoomPlayer rp : players) {
            GameSession session = rp.getSession();
            if (session == null || !session.isActive()) continue;

            // 延迟序列化 (只序列化一次, 所有玩家共享)
            if (payload == null) {
                payload = BattleFrameSerializer.serialize(frame,
                    com.actiongame.server.constant.GameConstants.PROTOCOL_VERSION);
            }

            var wrapper = com.actiongame.server.net.util.MessageHelper.wrap(
                com.actiongame.server.proto.MessageWrapperProto.MessageId.BATTLE_FRAME_NOTIFY,
                session.nextSequenceId(),
                payload
            );
            session.send(wrapper);
        }

        if (frame.getFrameIndex() % 50 == 0) {
            log.debug("Room {} frame {} broadcast: {} players, {} monsters, {} bytes",
                roomId, frame.getFrameIndex(), players.size(), monsters.size(),
                payload != null ? payload.length : 0);
        }
    }

    private void checkBattleEnd() {
        if (status.get() != RoomStatus.BATTLE) return;

        if (players.isEmpty()) {
            endBattle();
            return;
        }

        boolean allPlayersDead = players.stream().allMatch(p -> p.getCharacter().isDead());
        if (allPlayersDead) {
            endBattle();
            return;
        }

        if (monsters.isEmpty() && currentFrameIndex > 0) {
            endBattle();
        }
    }

    private RoomPlayer findPlayer(int entityId) {
        return playerByEntityId.get(entityId);
    }

    // === Getters ===

    public String getRoomId() { return roomId; }
    public RoomStatus getStatus() { return status.get(); }
    public List<RoomPlayer> getPlayers() { return players; }
    public List<RoomMonster> getMonsters() { return monsters; }
    public long getCurrentFrameIndex() { return currentFrameIndex; }
    public CombatSystem getCombatSystem() { return combatSystem; }
    public BuffEngine getBuffEngine(int entityId) { return buffEngines.get(entityId); }
    public Character getEntity(int entityId) { return entityMap.get(entityId); }
    public int getPlayerCount() { return players.size(); }
    public com.actiongame.server.anticheat.CheatDetector getCheatDetector() { return cheatDetector; }
    public com.actiongame.server.anticheat.CheatResponseManager getCheatResponseManager() { return cheatResponseManager; }

    private record PendingAction(int entityId, int actionType, float moveX, float moveZ,
                                  int skillId, int targetEntityId) {}
}
