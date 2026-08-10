package com.actiongame.server.room;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.persistence.InMemoryMatchResultRepository;
import com.actiongame.server.persistence.InMemoryRoomStateCache;
import com.actiongame.server.persistence.MatchResultRepository;
import com.actiongame.server.persistence.RoomState;
import com.actiongame.server.persistence.RoomStateCache;
import com.actiongame.server.persistence.StorageConfig;
import com.actiongame.server.persistence.StorageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间管理器 (对应文档3.5 RoomManager)
 * 管理所有战斗房间的创建、查找、销毁
 */
public class RoomManager {
    private static final Logger log = LoggerFactory.getLogger(RoomManager.class);

    private static RoomManager instance;

    private final Map<String, BattleRoom> rooms = new ConcurrentHashMap<>();
    private final MatchResultRepository matchResultRepository;
    private final RoomStateCache roomStateCache;

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

    /**
     * 创建房间
     */
    public BattleRoom createRoom(String roomId) {
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
