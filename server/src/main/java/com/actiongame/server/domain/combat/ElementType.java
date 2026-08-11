package com.actiongame.server.domain.combat;

/**
 * 元素类型 (对应Unity ElementType)
 * 克制关系: 火克冰、冰克雷、雷克水、水克火
 */
public enum ElementType {
    NONE(0),
    FIRE(1),
    ICE(2),
    LIGHTNING(3),
    WATER(4);

    private final int value;

    ElementType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ElementType fromValue(int value) {
        for (ElementType e : values()) {
            if (e.value == value) return e;
        }
        return NONE;
    }
}
