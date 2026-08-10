package com.actiongame.server.persistence;

public interface StringStore {
    void set(String key, String value, int ttlSeconds);
    String get(String key);
    void del(String key);
}
