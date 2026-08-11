package com.actiongame.server.ai.decision;

import com.actiongame.server.config.MonsterConfig;
import com.actiongame.server.domain.character.MonsterCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss AI控制器 (对应Unity BossController)
 * 多阶段HP阈值切换 + 狂暴模式
 * 继承EnemyAIController, 增加阶段管理
 */
public class BossAIController extends EnemyAIController {
    private static final Logger log = LoggerFactory.getLogger(BossAIController.class);

    private final List<BossPhase> phases = new ArrayList<>();
    private int currentPhaseIndex = 0;
    private boolean isEnraged = false;
    private float expReward = 500f;

    /**
     * Boss阶段定义 (对应Unity BossPhase)
     */
    public static class BossPhase {
        public final String name;
        public final float hpThreshold;
        public final float attackMultiplier;
        public final float moveSpeedMultiplier;
        public final float attackCooldownMultiplier;
        public final boolean isEnrage;

        public BossPhase(String name, float hpThreshold, float attackMultiplier,
                         float moveSpeedMultiplier, float attackCooldownMultiplier, boolean isEnrage) {
            this.name = name;
            this.hpThreshold = hpThreshold;
            this.attackMultiplier = attackMultiplier;
            this.moveSpeedMultiplier = moveSpeedMultiplier;
            this.attackCooldownMultiplier = attackCooldownMultiplier;
            this.isEnrage = isEnrage;
        }
    }

    public BossAIController(MonsterCharacter monster, MonsterConfig config) {
        super(monster, config);
        // 默认3阶段
        phases.add(new BossPhase("Phase 1", 1.0f, 1.0f, 1.0f, 1.0f, false));
        phases.add(new BossPhase("Phase 2", 0.66f, 1.3f, 1.2f, 0.8f, false));
        phases.add(new BossPhase("Phase 3 (Enrage)", 0.33f, 2.0f, 1.5f, 0.5f, true));
    }

    public BossAIController(MonsterCharacter monster, MonsterConfig config, List<BossPhase> customPhases) {
        super(monster, config);
        phases.addAll(customPhases);
    }

    /**
     * 每帧检查阶段切换
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        checkPhaseTransition();
    }

    private void checkPhaseTransition() {
        if (monster.isDead() || phases.isEmpty()) return;

        float hpPercent = monster.getStats().getHealthPercent();
        int targetPhase = currentPhaseIndex;

        for (int i = currentPhaseIndex + 1; i < phases.size(); i++) {
            if (hpPercent <= phases.get(i).hpThreshold) {
                targetPhase = i;
            } else {
                break;
            }
        }

        if (targetPhase != currentPhaseIndex) {
            transitionToPhase(targetPhase);
        }
    }

    private void transitionToPhase(int phaseIndex) {
        if (phaseIndex < 0 || phaseIndex >= phases.size()) return;

        currentPhaseIndex = phaseIndex;
        BossPhase phase = phases.get(phaseIndex);

        if (phase.isEnrage && !isEnraged) {
            isEnraged = true;
            monster.getStats().addAttackModifier(1.0f); // +100% attack
            log.info("Boss {} entered ENRAGE mode!", monster.getEntityId());
        }

        // 调整移速
        if (config != null) {
            moveSpeed = config.getMoveSpeed() * phase.moveSpeedMultiplier;
        }

        log.info("Boss {} transitioned to {} (phase {}/{})",
            monster.getEntityId(), phase.name, phaseIndex + 1, phases.size());
    }

    public int getCurrentPhaseIndex() { return currentPhaseIndex; }
    public BossPhase getCurrentPhase() {
        return phases.isEmpty() ? null : phases.get(currentPhaseIndex);
    }
    public boolean isEnraged() { return isEnraged; }
    public float getExpReward() { return expReward; }
    public float getCurrentAttackCooldownMultiplier() {
        BossPhase p = getCurrentPhase();
        return p != null ? p.attackCooldownMultiplier : 1f;
    }
}
