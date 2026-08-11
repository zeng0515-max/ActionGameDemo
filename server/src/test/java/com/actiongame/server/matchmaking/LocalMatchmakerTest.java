package com.actiongame.server.matchmaking;

import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.PlayerCharacter;
import com.actiongame.server.room.BattleRoom;
import com.actiongame.server.room.RoomManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("本地匹配器")
class LocalMatchmakerTest {

    @Test
    @DisplayName("should_createRoomWhenNoneExistsAndReuseRoomWithCapacity")
    void should_createRoomWhenNoneExistsAndReuseRoomWithCapacity() {
        RoomManager manager = new RoomManager();
        LocalMatchmaker matchmaker = new LocalMatchmaker(manager);

        String first = matchmaker.allocateRoom("p1");
        assertThat(first).startsWith("match-");
        assertThat(manager.getRoom(first)).isNotNull();

        String second = matchmaker.allocateRoom("p2");
        assertThat(second).isEqualTo(first);

        manager.destroyRoom(first);
    }

    @Test
    @DisplayName("should_pickLeastLoadedRoom")
    void should_pickLeastLoadedRoom() {
        RoomManager manager = new RoomManager();
        BattleRoom roomA = manager.createRoom("room-a");
        BattleRoom roomB = manager.createRoom("room-b");
        addPlayer(roomA, "a1");
        addPlayer(roomB, "b1");
        addPlayer(roomB, "b2");

        LocalMatchmaker matchmaker = new LocalMatchmaker(manager);
        assertThat(matchmaker.allocateRoom("p3")).isEqualTo("room-a");

        manager.destroyRoom("room-a");
        manager.destroyRoom("room-b");
    }

    private void addPlayer(BattleRoom room, String playerId) {
        CharacterStats stats = new CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        room.addPlayer(new PlayerCharacter(0, 1, stats, playerId), null);
    }
}
