package com.actiongame.server.persistence;

import java.util.Collection;

public interface StringStore {
    void set(String key, String value, int ttlSeconds);
    boolean setIfAbsent(String key, String value, int ttlSeconds);
    String get(String key);
    void del(String key);
    Collection<String> keys(String pattern);
}
