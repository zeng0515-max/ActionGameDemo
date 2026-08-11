package com.actiongame.server.domain.character;

/**
 * 角色状态 (对应Unity CharacterStateProto)
 */
public enum CharacterState {
    IDLE(0),
    MOVE(1),
    JUMP(2),
    ATTACK(3),
    SKILL(4),
    ULTIMATE(5),
    DODGE(6),
    HURT(7),
    DEAD(8);

    private final int value;

    CharacterState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CharacterState fromValue(int value) {
        for (CharacterState s : values()) {
            if (s.value == value) return s;
        }
        return IDLE;
    }
}
