package com.actiongame.server.battle.combatsystem;

import com.actiongame.server.battle.buffengine.BuffEngine;
import com.actiongame.server.battle.pipeline.DamageContext;
import com.actiongame.server.battle.pipeline.DamagePipeline;
import com.actiongame.server.config.BuffConfig;
import com.actiongame.server.domain.buff.Buff;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.domain.combat.DamageInfo;
import com.actiongame.server.domain.combat.HitResult;
import com.actiongame.server.util.Vector3;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 战斗系统主类 (对应Unity CombatCoordinator)
 * 协调伤害管线、命中检测、Buff引擎、连击系统
 */
public class CombatSystem {
    private final DamagePipeline damagePipeline;
    private final HitDetectionSystem hitDetection;

    /** BuffEngine 解析器: 由 BattleRoom 注入, 解析 entityId → BuffEngine */
    private Function<Integer, BuffEngine> buffEngineResolver;

    public CombatSystem() {
        this.damagePipeline = new DamagePipeline();
        this.hitDetection = new HitDetectionSystem();
    }

    /** 注入 BuffEngine 解析器 (由 BattleRoom 调用) */
    public void setBuffEngineResolver(Function<Integer, BuffEngine> resolver) {
        this.buffEngineResolver = resolver;
    }

    /**
     * 执行一次攻击: 命中检测 → 伤害计算 → 伤害结算
     */
    public HitResult executeAttack(Character attacker, Character target,
                                    float skillMultiplier,
                                    com.actiongame.server.domain.combat.ElementType attackElement) {
        if (target == null || target.isDead() || attacker == null) {
            return HitResult.miss(target != null ? target.getEntityId() : -1);
        }

        DamageInfo damageInfo = new DamageInfo();
        damageInfo.setSkillMultiplier(skillMultiplier);
        damageInfo.setAttackElement(attackElement);
        damageInfo.setDefenderElement(target.getElementType());
        damageInfo.setHitPosition(target.getPosition());

        DamageContext context = new DamageContext(
            attacker.getStats().getEffectiveAttackPower(),
            target.getStats().getEffectiveDefense(),
            attacker.getStats().getEffectiveCriticalRate(),
            attacker.getStats().getCriticalDamageMultiplier(),
            target.getStats().getDamageTakenMultiplier(),
            attacker.getEntityId(),
            target.getEntityId()
        );

        damagePipeline.calculate(damageInfo, context);
        target.takeDamage(damageInfo.getFinalDamage());

        return HitResult.hit(target.getEntityId(), damageInfo.getFinalDamage(),
            damageInfo.isCritical(), attackElement);
    }

    /**
     * 批量命中检测 + 伤害结算, 返回所有命中结果
     */
    public List<HitResult> executeAreaAttack(Character attacker, Vector3 center, float radius,
                                              List<Character> candidates, Set<Integer> alreadyHit,
                                              float skillMultiplier,
                                              com.actiongame.server.domain.combat.ElementType element) {
        List<Character> hits = hitDetection.detectHits(attacker, center, radius, candidates, alreadyHit);
        return hits.stream()
            .map(target -> executeAttack(attacker, target, skillMultiplier, element))
            .toList();
    }

    /**
     * 给目标添加Buff
     */
    public Buff applyBuff(Character target, BuffConfig buffConfig, Character source) {
        BuffEngine engine = getBuffEngine(target);
        if (engine == null) {
            return null;
        }
        return engine.addBuff(buffConfig, source);
    }

    /**
     * 更新角色Buff
     */
    public void updateBuffs(Character target, float deltaTime) {
        BuffEngine engine = getBuffEngine(target);
        if (engine != null) {
            engine.update(deltaTime);
        }
    }

    public DamagePipeline getDamagePipeline() { return damagePipeline; }
    public HitDetectionSystem getHitDetection() { return hitDetection; }

    private BuffEngine getBuffEngine(Character character) {
        if (buffEngineResolver != null && character != null) {
            return buffEngineResolver.apply(character.getEntityId());
        }
        return null;
    }
}
