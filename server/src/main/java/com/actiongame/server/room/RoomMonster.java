package com.actiongame.server.room;

import com.actiongame.server.ai.decision.EnemyAIController;
import com.actiongame.server.ai.decision.BossAIController;
import com.actiongame.server.config.MonsterConfig;
import com.actiongame.server.domain.character.MonsterCharacter;
import com.actiongame.server.domain.character.CharacterStats;

/**
 * 房间内怪物 (对应文档3.5 RoomMonster)
 * 封装怪物实体+AI控制器
 */
public class RoomMonster {
    private final MonsterCharacter character;
    private final EnemyAIController aiController;
    private boolean recycled;

    public RoomMonster(MonsterCharacter character, MonsterConfig config, boolean isBoss) {
        this.character = character;
        this.aiController = isBoss
            ? new BossAIController(character, config)
            : new EnemyAIController(character, config);
    }

    public MonsterCharacter getCharacter() { return character; }
    public EnemyAIController getAiController() { return aiController; }
    public int getEntityId() { return character.getEntityId(); }
    public boolean isRecycled() { return recycled; }
    public void setRecycled(boolean recycled) { this.recycled = recycled; }

    public void initialize() {
        aiController.initialize();
    }

    public void update(float deltaTime) {
        aiController.update(deltaTime);
    }
}
