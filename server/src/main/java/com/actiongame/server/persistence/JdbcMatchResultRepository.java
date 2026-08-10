package com.actiongame.server.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcMatchResultRepository implements MatchResultRepository {
    private static final Logger log = LoggerFactory.getLogger(JdbcMatchResultRepository.class);

    private static final String CREATE_TABLE_SQL =
        "CREATE TABLE IF NOT EXISTS battle_results (" +
        "room_id VARCHAR(64) NOT NULL, " +
        "start_time_ms BIGINT NOT NULL, " +
        "end_time_ms BIGINT NOT NULL, " +
        "total_frames BIGINT NOT NULL, " +
        "result INT NOT NULL, " +
        "player_count INT NOT NULL, " +
        "monster_count INT NOT NULL, " +
        "cheat_score INT NOT NULL, " +
        "PRIMARY KEY (room_id, start_time_ms))";

    private static final String INSERT_SQL =
        "INSERT INTO battle_results " +
        "(room_id, start_time_ms, end_time_ms, total_frames, result, player_count, monster_count, cheat_score) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ROOM_SQL =
        "SELECT room_id, start_time_ms, end_time_ms, total_frames, result, player_count, monster_count, cheat_score " +
        "FROM battle_results WHERE room_id = ? ORDER BY start_time_ms DESC";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM battle_results";

    private final DataSource dataSource;

    public JdbcMatchResultRepository(DataSource dataSource) {
        this.dataSource = dataSource;
        createTableIfNotExists();
    }

    @Override
    public void save(MatchResult result) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, result.getRoomId());
            statement.setLong(2, result.getStartTimeMs());
            statement.setLong(3, result.getEndTimeMs());
            statement.setLong(4, result.getTotalFrames());
            statement.setInt(5, result.getResult());
            statement.setInt(6, result.getPlayerCount());
            statement.setInt(7, result.getMonsterCount());
            statement.setInt(8, result.getCheatScore());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to save match result", e);
        }
    }

    @Override
    public List<MatchResult> findByRoomId(String roomId) {
        List<MatchResult> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ROOM_SQL)) {
            statement.setString(1, roomId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to query match results", e);
        }
        return results;
    }

    @Override
    public long count() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_SQL);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to count match results", e);
        }
    }

    private void createTableIfNotExists() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(CREATE_TABLE_SQL)) {
            statement.execute();
        } catch (SQLException e) {
            log.error("Failed to create battle_results table", e);
            throw new PersistenceException("Failed to create battle_results table", e);
        }
    }

    private MatchResult mapRow(ResultSet rs) throws SQLException {
        return new MatchResult(
            rs.getString("room_id"),
            rs.getLong("start_time_ms"),
            rs.getLong("end_time_ms"),
            rs.getLong("total_frames"),
            rs.getInt("result"),
            rs.getInt("player_count"),
            rs.getInt("monster_count"),
            rs.getInt("cheat_score")
        );
    }
}
