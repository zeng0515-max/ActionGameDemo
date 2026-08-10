package com.actiongame.server.room;

import com.actiongame.server.domain.character.PlayerCharacter;
import com.actiongame.server.net.session.GameSession;

/**
 * 房间内玩家 (对应文档3.5 RoomPlayer)
 */
public class RoomPlayer {
    private final int entityId;
    private final PlayerCharacter character;
    private final GameSession session;
    private boolean ready;

    public RoomPlayer(int entityId, PlayerCharacter character, GameSession session) {
        this.entityId = entityId;
        this.character = character;
        this.session = session;
    }

    public int getEntityId() { return entityId; }
    public PlayerCharacter getCharacter() { return character; }
    public GameSession getSession() { return session; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public String getPlayerId() { return character.getPlayerId(); }
}
