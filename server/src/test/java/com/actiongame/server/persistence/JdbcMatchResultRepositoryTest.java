package com.actiongame.server.persistence;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JDBC 对局结果仓库")
class JdbcMatchResultRepositoryTest {

    @Test
    @DisplayName("should_saveAndFind_when_h2Backed")
    void should_saveAndFind_when_h2Backed() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:match;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        JdbcMatchResultRepository repository = new JdbcMatchResultRepository(dataSource);
        repository.save(new MatchResult("room-jdbc", 100L, 500L, 20L, 2, 1, 4, 3));

        assertThat(repository.count()).isEqualTo(1);
        MatchResult saved = repository.findByRoomId("room-jdbc").get(0);
        assertThat(saved.getResult()).isEqualTo(2);
        assertThat(saved.getCheatScore()).isEqualTo(3);
    }
}
