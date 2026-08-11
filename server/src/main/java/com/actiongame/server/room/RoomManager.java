package com.actiongame.server.room;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.persistence.InMemoryMatchResultRepository;
import com.actiongame.server.persistence.InMemoryRoomStateCache;
import com.actiongame.server.persistence.InMemoryStringStore;
import com.actiongame.server.persistence.MatchResultRepository;
import com.actiongame.server.persistence.RoomState;
import com.actiongame.server.persistence.RoomStateCache;
import com.actiongame.server.persistence.StringStore;
import com.actiongame.server.persistence.StorageConfig;
import com.actiongame.server.persistence.StorageFactory;
import com.actiongame.server.routing.NodeRegistry;
import com.actiongame.server.routing.RoomRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 房间管理器 (对应文档3.5 RoomManager)
 * 管理所有战斗房间的创建、查找、销毁
 */
public class RoomManager {
    private static final Logger log = LoggerFactory.getLogger(RoomManager.class);
    private static final int NODE_TTL_SECONDS = 3600;
    private static final String DEFAULT_NODE_ID = "local";
    private static final String DEFAULT_NODE_ADDRESS = "localhost:9090";

    private static RoomManager instance;

    private final Map<String, BattleRoom> rooms = new ConcurrentHashMap<>();
    private final MatchResultRepository matchResultRepository;
    private final RoomStateCache roomStateCache;
    private final RoomRouter roomRouter;
    private final NodeRegistry nodeRegistry;
    private final AtomicBoolean draining = new AtomicBoolean(false);

    public static synchronized RoomManager getInstance() {
        if (instance == null) {
            StorageConfig config = StorageConfig.fromEnv();
            StringStore store = StorageFactory.createStringStore(config);
            String nodeId = System.getenv("NODE_ID");
            if (nodeId == null || nodeId.isBlank()) {
                nodeId = DEFAULT_NODE_ID;
            }
            String nodeAddress = resolveNodeAddress(
                nodeId,
                System.getenv("NODE_PUBLIC_ADDRESS"),
                System.getenv("NODE_ADDRESS_TEMPLATE"));
            instance = new RoomManager(
                StorageFactory.createMatchResultRepository(config),
                StorageFactory.createRoomStateCache(config),
                new RoomRouter(store, nodeId),
                new NodeRegistry(store, nodeId, nodeAddress, NODE_TTL_SECONDS)
            );
        }
        return instance;
    }

    public RoomManager() {
        this(new InMemoryMatchResultRepository(), new InMemoryRoomStateCache());
    }

    public RoomManager(MatchResultRepository matchResultRepository, RoomStateCache roomStateCache) {
        this(matchResultRepository, roomStateCache, new InMemoryStringStore(), DEFAULT_NODE_ID, DEFAULT_NODE_ADDRESS);
    }

    public RoomManager(MatchResultRepository matchResultRepository, RoomStateCache roomStateCache,
                       RoomRouter roomRouter, NodeRegistry nodeRegistry) {
        this.matchResultRepository = matchResultRepository;
        this.roomStateCache = roomStateCache;
        this.roomRouter = roomRouter;
        this.nodeRegistry = nodeRegistry;
        nodeRegistry.register();
    }

    private RoomManager(MatchResultRepository matchResultRepository, RoomStateCache roomStateCache,
                        StringStore store, String nodeId, String nodeAddress) {
        this(matchResultRepository, roomStateCache,
            new RoomRouter(store, nodeId),
            new NodeRegistry(store, nodeId, nodeAddress, NODE_TTL_SECONDS));
    }

    /**
     * 创建房间
     */
    public BattleRoom createRoom(String roomId) {
        if (draining.get()) {
            throw new IllegalStateException("Node is draining, new rooms are not accepted");
        }
        if (!roomRouter.tryAcquire(roomId)) {
            throw new IllegalStateException(
                "Room " + roomId + " is owned by node " + roomRouter.getOwner(roomId));
        }
        BattleRoom room = new BattleRoom(roomId, matchResultRepository, roomStateCache);
        rooms.put(roomId, room);
        roomStateCache.put(roomId, toRoomState(room));
        log.info("Room created: {}", roomId);
        return room;
    }

    /**
     * 获取或创建默认房间
     */
    public BattleRoom getOrCreateRoom(String roomId) {
        if (draining.get() && !rooms.containsKey(roomId)) {
            throw new IllegalStateException("Node is draining, new rooms are not accepted");
        }
        if (!roomRouter.tryAcquire(roomId)) {
            throw new IllegalStateException(
                "Room " + roomId + " is owned by node " + roomRouter.getOwner(roomId));
        }
        return rooms.computeIfAbsent(roomId, id -> {
            BattleRoom room = new BattleRoom(id, matchResultRepository, roomStateCache);
            roomStateCache.put(id, toRoomState(room));
            return room;
        });
    }

    /**
     * 获取房间
     */
    public BattleRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * 销毁房间
     */
    public void destroyRoom(String roomId) {
        BattleRoom room = rooms.remove(roomId);
        if (room != null) {
            room.endBattle();
            roomStateCache.remove(roomId);
            roomStateCache.removeSnapshot(roomId);
            roomRouter.releaseIfOwner(roomId);
            log.info("Room destroyed: {}", roomId);
        }
    }

    /**
     * 获取所有房间
     */
    public Collection<BattleRoom> getAllRooms() {
        return rooms.values();
    }

    /**
     * 房间数量
     */
    public int getRoomCount() {
        return rooms.size();
    }

    /**
     * 获取默认房间
     */
    public BattleRoom getDefaultRoom() {
        return getOrCreateRoom(GameConstants.DEFAULT_ROOM_ID);
    }

    /**
     * 清理已结束的房间 (SETTLEMENT/CLOSED 状态)
     */
    public int cleanupExpiredRooms() {
        int cleaned = 0;
        var entries = new java.util.ArrayList<>(rooms.entrySet());
        for (var entry : entries) {
            BattleRoom room = entry.getValue();
            RoomStatus status = room.getStatus();
            if (status == RoomStatus.SETTLEMENT || status == RoomStatus.CLOSED) {
                rooms.remove(entry.getKey());
                room.closeRoom();
                roomStateCache.remove(entry.getKey());
                roomStateCache.removeSnapshot(entry.getKey());
                roomRouter.releaseIfOwner(entry.getKey());
                cleaned++;
                log.info("Expired room cleaned: {}", entry.getKey());
            }
        }
        if (cleaned > 0) {
            log.info("Cleaned up {} expired rooms", cleaned);
        }
        return cleaned;
    }

    /**
     * 获取房间数量 (按状态)
     */
    public int getRoomCountByStatus(RoomStatus status) {
        return (int) rooms.values().stream().filter(r -> r.getStatus() == status).count();
    }

    public boolean tryAcquireRoom(String roomId) {
        if (draining.get() && rooms.get(roomId) == null) {
            return false;
        }
        return roomRouter.tryAcquire(roomId);
    }

    public void setDraining(boolean draining) {
        this.draining.set(draining);
        log.info("Room manager draining={}, activeRooms={}", draining, rooms.size());
    }

    public boolean isDraining() {
        return draining.get();
    }

    public String getRoomOwner(String roomId) {
        return roomRouter.getOwner(roomId);
    }

    public String resolveNodeAddress(String nodeId) {
        return nodeRegistry.resolve(nodeId);
    }

    public String getNodeId() {
        return roomRouter.getNodeId();
    }

    public NodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    public RoomRouter getRoomRouter() {
        return roomRouter;
    }

    public void start() {
        nodeRegistry.start();
    }

    public void stop() {
        nodeRegistry.stop();
    }

    public void migrateRoom(String roomId) {
        BattleRoom room = rooms.get(roomId);
        if (room == null) {
            log.warn("Cannot migrate missing room {}", roomId);
            return;
        }
        room.suspendForMigration();
        roomStateCache.putSnapshot(roomId, room.toSnapshot());
        roomRouter.releaseIfOwner(roomId);
        rooms.remove(roomId);
        log.info("Room {} migrated to snapshot store", roomId);
    }

    public int migrateAllRooms() {
        var roomIds = new java.util.ArrayList<>(rooms.keySet());
        for (String roomId : roomIds) {
            migrateRoom(roomId);
        }
        return roomIds.size();
    }

    public boolean restoreRoomIfAvailable(String roomId) {
        if (draining.get() || rooms.containsKey(roomId)) {
            return rooms.containsKey(roomId);
        }
        RoomSnapshot snapshot = roomStateCache.getSnapshot(roomId);
        if (snapshot == null || !roomRouter.tryAcquire(roomId)) {
            return false;
        }

        BattleRoom room = new BattleRoom(roomId, matchResultRepository, roomStateCache);
        room.restoreFromSnapshot(snapshot);
        rooms.put(roomId, room);
        room.resumeBattle();
        log.info("Room {} restored on node {}", roomId, getNodeId());
        return true;
    }

    public boolean releaseStaleRoom(String roomId) {
        String owner = roomRouter.getOwner(roomId);
        if (owner == null || owner.equals(getNodeId())) {
            return true;
        }
        String address = nodeRegistry.resolve(owner);
        if (address == null || address.isBlank()) {
            log.warn("Room {} owner {} is not alive, releasing stale ownership", roomId, owner);
            roomRouter.forceRelease(roomId);
            return true;
        }
        return false;
    }

    static String resolveNodeAddress(String nodeId, String nodeAddress, String addressTemplate) {
        if (nodeAddress != null && !nodeAddress.isBlank()) {
            return nodeAddress;
        }
        if (addressTemplate != null && !addressTemplate.isBlank()) {
            return addressTemplate.replace("${NODE_ID}", nodeId);
        }
        return DEFAULT_NODE_ADDRESS;
    }

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
}
