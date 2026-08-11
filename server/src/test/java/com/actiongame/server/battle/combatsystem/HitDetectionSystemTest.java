package com.actiongame.server.battle.combatsystem;

import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.MonsterCharacter;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.util.Vector3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("命中检测系统测试")
class HitDetectionSystemTest {

    private final HitDetectionSystem hitSystem = new HitDetectionSystem();

    @Test
    @DisplayName("should_detectTarget_when_inRange")
    void should_detectTarget_when_inRange() {
        // Given
        Character attacker = createCharacter(1, Vector3.ZERO);
        Character target = createCharacter(2, new Vector3(3, 0, 0));

        // When
        var hits = hitSystem.detectHits(attacker, Vector3.ZERO, 5f,
            List.of(target), new java.util.HashSet<>());

        // Then
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getEntityId()).isEqualTo(2);
    }

    @Test
    @DisplayName("should_notDetectTarget_when_outOfRange")
    void should_notDetectTarget_when_outOfRange() {
        // Given
        Character attacker = createCharacter(1, Vector3.ZERO);
        Character target = createCharacter(2, new Vector3(10, 0, 0));

        // When
        var hits = hitSystem.detectHits(attacker, Vector3.ZERO, 5f,
            List.of(target), new java.util.HashSet<>());

        // Then
        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("should_notDetectDeadTarget")
    void should_notDetectDeadTarget() {
        // Given
        Character attacker = createCharacter(1, Vector3.ZERO);
        Character target = createCharacter(2, new Vector3(1, 0, 0));
        target.takeDamage(9999f); // 击杀

        // When
        var hits = hitSystem.detectHits(attacker, Vector3.ZERO, 5f,
            List.of(target), new java.util.HashSet<>());

        // Then
        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("should_deduplicateHits_when_alreadyHit")
    void should_deduplicateHits_when_alreadyHit() {
        // Given
        Character attacker = createCharacter(1, Vector3.ZERO);
        Character target = createCharacter(2, new Vector3(1, 0, 0));
        Set<Integer> alreadyHit = new java.util.HashSet<>();
        alreadyHit.add(2);

        // When
        var hits = hitSystem.detectHits(attacker, Vector3.ZERO, 5f,
            List.of(target), alreadyHit);

        // Then
        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("should_notDetectSelf")
    void should_notDetectSelf() {
        // Given
        Character attacker = createCharacter(1, Vector3.ZERO);

        // When
        var hits = hitSystem.detectHits(attacker, Vector3.ZERO, 5f,
            List.of(attacker), new java.util.HashSet<>());

        // Then
        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("should_returnTrue_when_isInRange")
    void should_returnTrue_when_isInRange() {
        // Given
        Character attacker = createCharacter(1, Vector3.ZERO);
        Character target = createCharacter(2, new Vector3(3, 0, 0));

        // When & Then
        assertThat(hitSystem.isInRange(attacker, target, 5f)).isTrue();
        assertThat(hitSystem.isInRange(attacker, target, 2f)).isFalse();
    }

    private Character createCharacter(int entityId, Vector3 pos) {
        CharacterStats stats = new CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        Character c = new MonsterCharacter(entityId, 1, stats);
        c.setPosition(pos);
        return c;
    }
}
