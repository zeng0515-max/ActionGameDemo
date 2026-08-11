package com.actiongame.server.battle.buffengine;

import com.actiongame.server.config.BuffConfig;
import com.actiongame.server.domain.buff.AttributeType;
import com.actiongame.server.domain.buff.BuffStackingRule;
import com.actiongame.server.domain.buff.BuffType;
import com.actiongame.server.domain.character.CharacterStats;
import com.actiongame.server.domain.character.MonsterCharacter;
import com.actiongame.server.domain.character.PlayerCharacter;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.domain.combat.ElementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Buff引擎测试")
class BuffEngineTest {

    @Test
    @DisplayName("should_addBuff_when_newBuffApplied")
    void should_addBuff_when_newBuffApplied() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        Character target = new MonsterCharacter(1, 1, stats);
        BuffEngine engine = new BuffEngine(target);

        BuffConfig config = new BuffConfig();
        config.setBuffName("Attack Up");
        config.setBuffType(BuffType.ATTRIBUTE);
        config.setDuration(10f);
        config.setStackingRule(BuffStackingRule.REFRESH_DURATION);
        config.setMaxStacks(1);
        config.setAttributeType(AttributeType.ATTACK_POWER);
        config.setAttributeModifier(20f);

        // When
        engine.addBuff(config, null);

        // Then: 攻击力应提升20% → 20 × 1.2 = 24
        assertThat(engine.getBuffCount()).isEqualTo(1);
        assertThat(stats.getEffectiveAttackPower()).isEqualTo(24f, within(0.001f));
    }

    @Test
    @DisplayName("should_removeBuffEffect_when_buffRemoved")
    void should_removeBuffEffect_when_buffRemoved() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        Character target = new MonsterCharacter(1, 1, stats);
        BuffEngine engine = new BuffEngine(target);

        BuffConfig config = new BuffConfig();
        config.setBuffName("Defense Up");
        config.setBuffType(BuffType.ATTRIBUTE);
        config.setDuration(10f);
        config.setStackingRule(BuffStackingRule.REFRESH_DURATION);
        config.setMaxStacks(1);
        config.setAttributeType(AttributeType.DEFENSE);
        config.setAttributeModifier(30f);

        // When
        var buff = engine.addBuff(config, null);
        assertThat(stats.getEffectiveDefense()).isEqualTo(6.5f, within(0.001f)); // 5 × 1.3 = 6.5

        engine.removeBuff(buff);

        // Then: 防御力应恢复
        assertThat(stats.getEffectiveDefense()).isEqualTo(5f, within(0.001f));
        assertThat(engine.getBuffCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should_refreshDuration_when_sameBuffAddedTwice")
    void should_refreshDuration_when_sameBuffAddedTwice() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        Character target = new MonsterCharacter(1, 1, stats);
        BuffEngine engine = new BuffEngine(target);

        BuffConfig config = new BuffConfig();
        config.setBuffName("Attack Up");
        config.setBuffType(BuffType.ATTRIBUTE);
        config.setDuration(5f);
        config.setStackingRule(BuffStackingRule.REFRESH_DURATION);
        config.setMaxStacks(1);
        config.setAttributeType(AttributeType.ATTACK_POWER);
        config.setAttributeModifier(20f);

        // When
        engine.addBuff(config, null);
        engine.addBuff(config, null); // 刷新持续时间

        // Then: 应只有1个Buff, 效果不叠加
        assertThat(engine.getBuffCount()).isEqualTo(1);
        assertThat(stats.getEffectiveAttackPower()).isEqualTo(24f, within(0.001f)); // 20×1.2, 不是20×1.4
    }

    @Test
    @DisplayName("should_stackStacks_when_stackRuleApplied")
    void should_stackStacks_when_stackRuleApplied() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        Character target = new MonsterCharacter(1, 1, stats);
        BuffEngine engine = new BuffEngine(target);

        BuffConfig config = new BuffConfig();
        config.setBuffName("Attack Up");
        config.setBuffType(BuffType.ATTRIBUTE);
        config.setDuration(10f);
        config.setStackingRule(BuffStackingRule.STACK_STACKS);
        config.setMaxStacks(3);
        config.setAttributeType(AttributeType.ATTACK_POWER);
        config.setAttributeModifier(10f); // 每层+10%

        // When
        engine.addBuff(config, null); // 1层: 20×1.1=22
        engine.addBuff(config, null); // 2层: 20×1.2=24
        engine.addBuff(config, null); // 3层: 20×1.3=26

        // Then
        assertThat(engine.getBuffCount()).isEqualTo(1);
        var buff = engine.findBuffByName("Attack Up");
        assertThat(buff.getStacks()).isEqualTo(3);
        assertThat(stats.getEffectiveAttackPower()).isEqualTo(26f, within(0.001f));
    }

    @Test
    @DisplayName("should_applyShield_when_shieldBuffApplied")
    void should_applyShield_when_shieldBuffApplied() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        Character target = new MonsterCharacter(1, 1, stats);
        BuffEngine engine = new BuffEngine(target);

        BuffConfig config = new BuffConfig();
        config.setBuffName("Shield");
        config.setBuffType(BuffType.SHIELD);
        config.setDuration(15f);
        config.setStackingRule(BuffStackingRule.STACK_STACKS);
        config.setMaxStacks(3);
        config.setShieldValue(50f);

        // When
        engine.addBuff(config, null);

        // Then
        assertThat(target.getShieldAmount()).isEqualTo(50f, within(0.001f));
    }

    @Test
    @DisplayName("should_expireBuff_when_durationReached")
    void should_expireBuff_when_durationReached() {
        // Given
        CharacterStats stats = new CharacterStats(100f, 20f, 5f, 5f, 0.1f, 1.5f);
        Character target = new MonsterCharacter(1, 1, stats);
        BuffEngine engine = new BuffEngine(target);

        BuffConfig config = new BuffConfig();
        config.setBuffName("Attack Up");
        config.setBuffType(BuffType.ATTRIBUTE);
        config.setDuration(2f);
        config.setStackingRule(BuffStackingRule.REFRESH_DURATION);
        config.setMaxStacks(1);
        config.setAttributeType(AttributeType.ATTACK_POWER);
        config.setAttributeModifier(20f);

        engine.addBuff(config, null);
        assertThat(engine.getBuffCount()).isEqualTo(1);

        // When: 模拟超过持续时间
        engine.update(2.5f);

        // Then: Buff应被移除, 属性恢复
        assertThat(engine.getBuffCount()).isEqualTo(0);
        assertThat(stats.getEffectiveAttackPower()).isEqualTo(20f, within(0.001f));
    }
}
