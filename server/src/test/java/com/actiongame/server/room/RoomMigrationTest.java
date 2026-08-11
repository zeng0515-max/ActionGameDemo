package com.actiongame.server.room;

import com.actiongame.server.config.MonsterConfig;
import com.actiongame.server.config.BuffConfig;
import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.PlayerCharacter;
import com.actiongame.server.domain.buff.AttributeType;
import com.actiongame.server.domain.buff.BuffType;
import com.actiongame.server.net.session.GameSession;
import com.actiongame.server.persistence.InMemoryMatchResultRepository;
import com.actiongame.server.persistence.InMemoryRoomStateCache;
import com.actiongame.server.persistence.InMemoryStringStore;
import com.actiongame.server.persistence.StringStore;
import com.actiongame.server.routing.NodeRegistry;
import com.actiongame.server.routing.RoomRouter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("房间迁移")
class RoomMigrationTest {

    @Test
    @DisplayName("should_restoreMigratedRoomOnAnotherNode")
    void should_restoreMigratedRoomOnAnotherNode() {
        StringStore store = new InMemoryStringStore();
        InMemoryRoomStateCache cache = new InMemoryRoomStateCache();
        RoomManager nodeA = new RoomManager(
            new InMemoryMatchResultRepository(),
            cache,
            new RoomRouter(store, "node-a"),
            new NodeRegistry(store, "node-a", "game-a:9090", 3600));
        RoomManager nodeB = new RoomManager(
            new InMemoryMatchResultRepository(),
            cache,
            new RoomRouter(store, "node-b"),
            new NodeRegistry(store, "node-b", "game-b:9090", 3600));

        BattleRoom room = nodeA.createRoom("migrate-1");
        EmbeddedChannel channel = new EmbeddedChannel();
        GameSession session = new GameSession("p1", channel);
        CharacterStats stats = new CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        int playerEntityId = room.addPlayer(new PlayerCharacter(0, 1, stats, "p1"), session);
        BuffConfig buffConfig = new BuffConfig();
        buffConfig.setBuffName("Attack Up");
        buffConfig.setBuffType(BuffType.ATTRIBUTE);
        buffConfig.setDuration(10f);
        buffConfig.setAttributeType(AttributeType.ATTACK_POWER);
        buffConfig.setAttributeModifier(50f);
        room.getBuffEngine(playerEntityId).addBuff(buffConfig, null);
        room.spawnMonster(new MonsterConfig(), false);
        room.startBattle();

        nodeA.migrateRoom("migrate-1");
        assertThat(nodeA.getRoom("migrate-1")).isNull();
        assertThat(nodeB.getRoomOwner("migrate-1")).isNull();

        assertThat(nodeB.restoreRoomIfAvailable("migrate-1")).isTrue();
        BattleRoom restored = nodeB.getRoom("migrate-1");
        assertThat(restored).isNotNull();
        assertThat(restored.getPlayerCount()).isEqualTo(1);
        assertThat(restored.getMonsters().size()).isEqualTo(1);
        assertThat(restored.getStatus()).isEqualTo(RoomStatus.BATTLE);
        assertThat(restored.getBuffEngine(playerEntityId).getBuffCount()).isEqualTo(1);
        assertThat(restored.getEntity(playerEntityId).getStats().getEffectiveAttackPower()).isGreaterThan(10f);

        nodeB.destroyRoom("migrate-1");
        channel.finishAndReleaseAll();
    }
}
