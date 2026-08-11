package com.actiongame.server.domain.buff;

/**
 * Buff大类型 (对应Unity BuffType)
 */
public enum BuffType {
    ATTRIBUTE(1),
    DOT(2),
    CONTROL(3),
    SHIELD(4);

    private final int value;

    BuffType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static BuffType fromValue(int value) {
        for (BuffType b : values()) {
            if (b.value == value) return b;
        }
        return ATTRIBUTE;
    }
}
