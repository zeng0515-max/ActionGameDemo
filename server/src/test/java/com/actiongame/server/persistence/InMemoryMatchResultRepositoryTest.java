package com.actiongame.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("内存对局结果仓库")
class InMemoryMatchResultRepositoryTest {

    @Test
    @DisplayName("should_saveAndFind_when_matchResultSaved")
    void should_saveAndFind_when_matchResultSaved() {
        InMemoryMatchResultRepository repository = new InMemoryMatchResultRepository();

        repository.save(new MatchResult("room-1", 1000L, 5000L, 200L, 1, 2, 3, 0));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByRoomId("room-1")).hasSize(1);
        MatchResult saved = repository.findByRoomId("room-1").get(0);
        assertThat(saved.getRoomId()).isEqualTo("room-1");
        assertThat(saved.getTotalFrames()).isEqualTo(200L);
        assertThat(saved.getResult()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_returnEmpty_when_roomHasNoMatch")
    void should_returnEmpty_when_roomHasNoMatch() {
        InMemoryMatchResultRepository repository = new InMemoryMatchResultRepository();

        assertThat(repository.findByRoomId("missing")).isEmpty();
        assertThat(repository.count()).isZero();
    }
}
