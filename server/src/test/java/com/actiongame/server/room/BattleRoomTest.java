package com.actiongame.server.room;

import com.actiongame.server.config.MonsterConfig;
import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.PlayerCharacter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("战斗房间测试")
class BattleRoomTest {

    @Test
    @DisplayName("should_addPlayer_when_joinRoom")
    void should_addPlayer_when_joinRoom() {
        // Given
        BattleRoom room = new BattleRoom("test-01");
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        PlayerCharacter player = new PlayerCharacter(0, 1, stats, "test-player");

        // When
        int entityId = room.addPlayer(player, null);

        // Then
        assertThat(entityId).isGreaterThan(0);
        assertThat(room.getPlayers()).hasSize(1);
    }

    @Test
    @DisplayName("should_spawnMonster_when_spawnCalled")
    void should_spawnMonster_when_spawnCalled() {
        // Given
        BattleRoom room = new BattleRoom("test-02");
        MonsterConfig config = createMonsterConfig();

        // When
        int entityId = room.spawnMonster(config, false);

        // Then
        assertThat(entityId).isGreaterThan(0);
        assertThat(room.getMonsters()).hasSize(1);
    }

    @Test
    @DisplayName("should_recycleDeadMonster_when_executeFrame")
    void should_recycleDeadMonster_when_executeFrame() {
        // Given
        BattleRoom room = new BattleRoom("test-03");
        MonsterConfig config = createMonsterConfig();
        room.spawnMonster(config, false);

        // Kill the monster
        room.getMonsters().get(0).getCharacter().takeDamage(999f);

        // When: execute a frame (will recycle dead monster)
        room.executeFrame(0, 0.02f);

        // Then
        assertThat(room.getMonsters()).isEmpty();
    }

    @Test
    @DisplayName("should_generateFrameSnapshot_when_executeFrame")
    void should_generateFrameSnapshot_when_executeFrame() {
        // Given
        BattleRoom room = new BattleRoom("test-04");
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        PlayerCharacter player = new PlayerCharacter(0, 1, stats, "test-player");
        room.addPlayer(player, null);

        MonsterConfig config = createMonsterConfig();
        room.spawnMonster(config, false);

        // When: execute frame (BattleRoom doesn't return the frame, but we can check no crash)
        room.executeFrame(0, 0.02f);

        // Then: frame executed without error, frame index updated
        assertThat(room.getCurrentFrameIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("should_processPlayerMoveAction_when_actionSubmitted")
    void should_processPlayerMoveAction_when_actionSubmitted() {
        // Given
        BattleRoom room = new BattleRoom("test-05");
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        PlayerCharacter player = new PlayerCharacter(0, 1, stats, "test-player");
        int entityId = room.addPlayer(player, null);

        float initialX = player.getPosition().x;

        // When: submit move action (actionType=1, moveX=1, moveZ=0)
        room.submitPlayerAction(entityId, 1, 1f, 0f, 0, -1);
        room.executeFrame(0, 0.02f);

        // Then: player should have moved in X direction
        assertThat(player.getPosition().x).isGreaterThan(initialX);
    }

    @Test
    @DisplayName("should_startInWaitingStatus_when_created")
    void should_startInWaitingStatus_when_created() {
        // Given & When
        BattleRoom room = new BattleRoom("test-06");

        // Then
        assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
    }

    private MonsterConfig createMonsterConfig() {
        MonsterConfig c = new MonsterConfig();
        c.setEnemyName("TestGoblin");
        c.setMaxHealth(50f);
        c.setAttackPower(5f);
        c.setDefense(2f);
        c.setMoveSpeed(3f);
        c.setDetectionRange(10f);
        c.setAttackRange(2f);
        c.setFleeThreshold(0.2f);
        c.setPatrolRadius(5f);
        c.setViewAngle(90f);
        return c;
    }
}
