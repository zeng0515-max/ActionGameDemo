package com.actiongame.server.persistence;

import java.util.List;

public interface MatchResultRepository {
    void save(MatchResult result);
    List<MatchResult> findByRoomId(String roomId);
    long count();
}
