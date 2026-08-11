package com.actiongame.server.ai.decision;

import com.actiongame.server.ai.fsm.EnemyStateType;
import com.actiongame.server.config.MonsterConfig;
import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.MonsterCharacter;
import com.actiongame.server.domain.character.PlayerCharacter;
import com.actiongame.server.util.Vector3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("敌人AI状态机测试")
class EnemyAIControllerTest {

    @Test
    @DisplayName("should_startInIdleState_when_initialized")
    void should_startInIdleState_when_initialized() {
        // Given
        MonsterCharacter monster = createMonster(Vector3.ZERO);
        MonsterConfig config = createConfig();
        EnemyAIController ai = new EnemyAIController(monster, config);

        // When
        ai.initialize();

        // Then
        assertThat(ai.getCurrentStateType()).isEqualTo(EnemyStateType.IDLE);
    }

    @Test
    @DisplayName("should_switchToChase_when_targetDetected")
    void should_switchToChase_when_targetDetected() {
        // Given
        MonsterCharacter monster = createMonster(Vector3.ZERO);
        MonsterConfig config = createConfig();
        EnemyAIController ai = new EnemyAIController(monster, config);
        ai.initialize();

        // Set target within detection range
        CharacterStats playerStats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        PlayerCharacter player = new PlayerCharacter(2, 1, playerStats, "test-player");
        player.setPosition(new Vector3(5, 0, 0));
        ai.setTarget(player);

        // When: update AI — IdleState checkSwitchState should detect target
        ai.update(0.02f);

        // Then
        assertThat(ai.getCurrentStateType()).isEqualTo(EnemyStateType.CHASE);
    }

    @Test
    @DisplayName("should_switchToFlee_when_healthLow")
    void should_switchToFlee_when_healthLow() {
        // Given
        MonsterCharacter monster = createMonster(Vector3.ZERO);
        monster.getStats().modifyHealth(-80f); // HP: 100→20, fleeThreshold=0.2 → 20% = shouldFlee

        MonsterConfig config = createConfig();
        EnemyAIController ai = new EnemyAIController(monster, config);
        ai.initialize();

        CharacterStats playerStats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        PlayerCharacter player = new PlayerCharacter(2, 1, playerStats, "test-player");
        player.setPosition(new Vector3(3, 0, 0));
        ai.setTarget(player);

        // When: IdleState will detect target (CHASE) then check shouldFlee (FLEE)
        // Actually: IdleState checks detectTarget first → CHASE. ChaseState then checks shouldFlee → FLEE
        ai.update(0.02f); // Idle → Chase
        ai.update(0.02f); // Chase → Flee (shouldFlee)

        // Then
        assertThat(ai.getCurrentStateType()).isEqualTo(EnemyStateType.FLEE);
    }

    @Test
    @DisplayName("should_switchToDead_when_healthZero")
    void should_switchToDead_when_healthZero() {
        // Given
        MonsterCharacter monster = createMonster(Vector3.ZERO);
        monster.takeDamage(999f); // Kill

        MonsterConfig config = createConfig();
        EnemyAIController ai = new EnemyAIController(monster, config);
        ai.initialize();

        // When
        ai.update(0.02f);

        // Then
        assertThat(ai.getCurrentStateType()).isEqualTo(EnemyStateType.DEAD);
    }

    @Test
    @DisplayName("should_switchToHurt_when_onHurtCalled")
    void should_switchToHurt_when_onHurtCalled() {
        // Given
        MonsterCharacter monster = createMonster(Vector3.ZERO);
        MonsterConfig config = createConfig();
        EnemyAIController ai = new EnemyAIController(monster, config);
        ai.initialize();

        // When
        ai.onHurt();

        // Then
        assertThat(ai.getCurrentStateType()).isEqualTo(EnemyStateType.HURT);
    }

    private MonsterCharacter createMonster(Vector3 pos) {
        CharacterStats stats = new CharacterStats(100f, 5f, 2f, 3f, 0.05f, 1.5f);
        MonsterCharacter m = new MonsterCharacter(1, 1, stats);
        m.setPosition(pos);
        return m;
    }

    private MonsterConfig createConfig() {
        MonsterConfig c = new MonsterConfig();
        c.setEnemyName("TestGoblin");
        c.setMaxHealth(100f);
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
