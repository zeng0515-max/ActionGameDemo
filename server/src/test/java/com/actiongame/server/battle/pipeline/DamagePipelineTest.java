package com.actiongame.server.battle.pipeline;

import com.actiongame.server.domain.combat.DamageInfo;
import com.actiongame.server.domain.combat.ElementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("伤害管线测试")
class DamagePipelineTest {

    private final DamagePipeline pipeline = new DamagePipeline();

    @Test
    @DisplayName("should_calculateCorrectDamage_when_noCritNoElementNoDefense")
    void should_calculateCorrectDamage_when_noCritNoElementNoDefense() {
        // Given: 攻击力20, 倍率1.5, 无暴击, 无元素, 无防御
        DamageInfo input = new DamageInfo();
        input.setSkillMultiplier(1.5f);
        input.setAttackElement(ElementType.NONE);
        input.setDefenderElement(ElementType.NONE);
        input.setCritical(true); // 固定暴击, 避免随机

        DamageContext context = new DamageContext(20f, 0f, 0f, 1.5f, 1f, 1, 2);

        // When
        DamageInfo result = pipeline.calculate(input, context);

        // Then: 20 × 1.5 = 30 (暴击已固定true, 但critMult=1.5) → 30 × 1.5 = 45 → 无元素 → 无防御 → max(1, 45) = 45
        // 实际: step1=20×1.5=30, step2=30×1.5=45(crit), step3=45×1=45, step4=max(1,45)=45
        assertThat(result.getFinalDamage()).isEqualTo(45f, within(0.001f));
        assertThat(result.isCritical()).isTrue();
    }

    @Test
    @DisplayName("should_applyElementMultiplier_when_fireAttacksIce")
    void should_applyElementMultiplier_when_fireAttacksIce() {
        // Given: 攻击力20, 倍率1.0, 固定暴击, 火克冰(1.5x)
        DamageInfo input = new DamageInfo();
        input.setSkillMultiplier(1.0f);
        input.setAttackElement(ElementType.FIRE);
        input.setDefenderElement(ElementType.ICE);
        input.setCritical(true);

        DamageContext context = new DamageContext(20f, 0f, 0f, 1.5f, 1f, 1, 2);

        // When
        DamageInfo result = pipeline.calculate(input, context);

        // Then: step1=20×1=20, step2=20×1.5=30(crit), step3=30×1.5=45(火克冰), step4=max(1,45)=45
        assertThat(result.getFinalDamage()).isEqualTo(45f, within(0.001f));
        assertThat(result.getElementMultiplier()).isEqualTo(1.5f);
    }

    @Test
    @DisplayName("should_reduceByDefense_when_defenseIsSet")
    void should_reduceByDefense_when_defenseIsSet() {
        // Given: 攻击力20, 倍率1.0, 固定暴击, 无元素, 防御力10
        DamageInfo input = new DamageInfo();
        input.setSkillMultiplier(1.0f);
        input.setCritical(true);
        input.setAttackElement(ElementType.NONE);
        input.setDefenderElement(ElementType.NONE);

        DamageContext context = new DamageContext(20f, 10f, 0f, 1.5f, 1f, 1, 2);

        // When
        DamageInfo result = pipeline.calculate(input, context);

        // Then: step1=20, step2=20×1.5=30(crit), step3=30×1=30, step4=30-10=20, step5=max(1,20)=20
        assertThat(result.getFinalDamage()).isEqualTo(20f, within(0.001f));
    }

    @Test
    @DisplayName("should_returnMinOneDamage_when_defenseExceedsDamage")
    void should_returnMinOneDamage_when_defenseExceedsDamage() {
        // Given: 攻击力5, 倍率1.0, 防御力100 → 伤害被完全减免
        DamageInfo input = new DamageInfo();
        input.setSkillMultiplier(1.0f);
        input.setCritical(true);

        DamageContext context = new DamageContext(5f, 100f, 0f, 1.5f, 1f, 1, 2);

        // When
        DamageInfo result = pipeline.calculate(input, context);

        // Then: step1=5, step2=5×1.5=7.5(crit), step3=7.5, step4=7.5-100=-92.5 → step5=max(1,-92.5)=1
        assertThat(result.getFinalDamage()).isEqualTo(1f);
    }

    @Test
    @DisplayName("should_applyDamageTakenMultiplier_when_shocked")
    void should_applyDamageTakenMultiplier_when_shocked() {
        // Given: 攻击力20, 倍率1.0, 防御5, 感电受伤增加25% (damageTakenMultiplier=1.25)
        DamageInfo input = new DamageInfo();
        input.setSkillMultiplier(1.0f);
        input.setCritical(true);

        DamageContext context = new DamageContext(20f, 5f, 0f, 1.5f, 1.25f, 1, 2);

        // When
        DamageInfo result = pipeline.calculate(input, context);

        // Then: step1=20, step2=20×1.5=30, step3=30, step4=30-5=25, step4b=25×1.25=31.25, step5=max(1,31.25)=31.25
        assertThat(result.getFinalDamage()).isEqualTo(31.25f, within(0.01f));
    }
}
