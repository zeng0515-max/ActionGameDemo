package com.actiongame.server.domain.character;

/**
 * 玩家角色 (服务端权威)
 */
public class PlayerCharacter extends Character {

    private String playerId;
    private int level = 1;
    private float currentExp = 0f;
    private int availableSkillPoints = 0;

    public PlayerCharacter(int entityId, int configId, CharacterStats stats, String playerId) {
        super(entityId, configId, stats);
        this.playerId = playerId;
    }

    public void addExperience(float exp) {
        currentExp += exp;
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public float getCurrentExp() { return currentExp; }
    public void setCurrentExp(float currentExp) { this.currentExp = currentExp; }
    public int getAvailableSkillPoints() { return availableSkillPoints; }
    public void setAvailableSkillPoints(int availableSkillPoints) { this.availableSkillPoints = availableSkillPoints; }
}
