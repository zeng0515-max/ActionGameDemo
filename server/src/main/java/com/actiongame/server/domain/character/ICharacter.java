package com.actiongame.server.domain.character;

/**
 * 角色接口
 */
public interface ICharacter {
    int getEntityId();
    int getConfigId();
    CharacterStats getStats();
    CharacterState getState();
    boolean isDead();
    void takeDamage(float damage);
    void heal(float amount);
}
