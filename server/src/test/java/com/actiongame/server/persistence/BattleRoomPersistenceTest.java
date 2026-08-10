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
