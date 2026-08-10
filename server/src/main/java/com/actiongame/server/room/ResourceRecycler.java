package com.actiongame.server.room;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 资源回收器 (对应文档3.5 ResourceRecycler)
 * 死亡实体回收、过期Buff清理、房间销毁
 */
public class ResourceRecycler {

    /**
     * 回收死亡怪物
     */
    public List<RoomMonster> recycleDeadMonsters(List<RoomMonster> monsters) {
        List<RoomMonster> recycled = new ArrayList<>();
        List<RoomMonster> toRemove = new ArrayList<>();
        for (RoomMonster rm : monsters) {
            if (rm.getCharacter().isDead() && !rm.isRecycled()) {
                rm.setRecycled(true);
                recycled.add(rm);
                toRemove.add(rm);
            }
        }
        if (!toRemove.isEmpty()) {
            monsters.removeAll(toRemove);
        }
        return recycled;
    }

    /**
     * 检查并回收超时死亡实体
     */
    public List<RoomMonster> recycleTimedOutMonsters(List<RoomMonster> monsters, int deadFrameCount) {
        return recycleDeadMonsters(monsters);
    }

    /**
     * 清理离线玩家, 同时回调通知房间清理关联资源
     * @param players 玩家列表
     * @param onRemove 回调, 传入被移除玩家的 entityId
     */
    public List<RoomPlayer> recycleOfflinePlayers(List<RoomPlayer> players, IntConsumer onRemove) {
        List<RoomPlayer> removed = new ArrayList<>();
        List<RoomPlayer> toRemove = new ArrayList<>();
        for (RoomPlayer rp : players) {
            if (rp.getSession() == null || !rp.getSession().isActive()) {
                removed.add(rp);
                toRemove.add(rp);
            }
        }
        if (!toRemove.isEmpty()) {
            players.removeAll(toRemove);
            for (RoomPlayer rp : toRemove) {
                onRemove.accept(rp.getEntityId());
            }
        }
        return removed;
    }
}
