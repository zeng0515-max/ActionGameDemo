package com.actiongame.server.anticheat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 反作弊检测器单元测试
 */
class CheatDetectorTest {

    private CheatDetector detector;

    @BeforeEach
    void setUp() {
        detector = new CheatDetector(0.02f); // 20ms deltaTime
    }

    // === Layer 1: 实时检测 ===

    @Test
    @DisplayName("Layer1: 正常移动不触发检测")
    void testNormalMove() {
        // 正常输入: moveX=1, moveZ=0 (长度=1, 在范围内)
        boolean detected = detector.checkRealtime(1, 1, 1.0f, 0.0f, System.currentTimeMillis(), 5f);
        assertFalse(detected, "Normal move should not trigger");
    }

    @Test
    @DisplayName("Layer1: 输入向量超限触发速度检测")
    void testSpeedHackInput() {
        // 输入向量长度 > 1.5 (moveX=2, moveZ=2 → 长度=2.83)
        boolean detected = detector.checkRealtime(1, 1, 2.0f, 2.0f, System.currentTimeMillis(), 5f);
        assertTrue(detected, "Oversized input should trigger speed hack");
    }

    @Test
    @DisplayName("Layer1: 攻击频率正常不触发")
    void testNormalAttackRate() {
        long t1 = System.currentTimeMillis();
        detector.checkRealtime(1, 3, 0, 0, t1, 5f);
        // 等待确保时间间隔 > 0.15s
        try { Thread.sleep(200); } catch (InterruptedException e) {}
        long t2 = System.currentTimeMillis();
        boolean detected = detector.checkRealtime(1, 3, 0, 0, t2, 5f);
        assertFalse(detected, "Normal attack rate should not trigger (t2-t1=" + (t2-t1) + "ms)");
    }

    @Test
    @DisplayName("Layer1: 攻击频率过高触发检测")
    void testAttackRateHack() {
        long now = System.currentTimeMillis();
        // 快速连续攻击 (间隔 < 0.15s)
        detector.checkRealtime(1, 3, 0, 0, now, 5f);
        boolean detected = detector.checkRealtime(1, 3, 0, 0, now + 100, 5f); // 0.1s间隔 < 0.15s
        assertTrue(detected, "Rapid attack should trigger");
    }

    // === Layer 2: 逻辑校验 ===

    @Test
    @DisplayName("Layer2: 正常伤害不触发")
    void testNormalDamage() {
        com.actiongame.server.domain.character.CharacterStats stats =
            new com.actiongame.server.domain.character.CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        var attacker = new com.actiongame.server.domain.character.PlayerCharacter(1, 1, stats, "p1");
        var target = new com.actiongame.server.domain.character.MonsterCharacter(2, 1,
            new com.actiongame.server.domain.character.CharacterStats(50f, 5f, 2f, 3f, 0.05f, 1.5f));

        // 正常伤害 10 * 1.0 = 10
        boolean detected = detector.checkLogic(attacker, target, 10f);
        assertFalse(detected, "Normal damage should not trigger");
    }

    @Test
    @DisplayName("Layer2: 伤害越界触发检测")
    void testDamageHack() {
        var stats = new com.actiongame.server.domain.character.CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        var attacker = new com.actiongame.server.domain.character.PlayerCharacter(1, 1, stats, "p1");
        var target = new com.actiongame.server.domain.character.MonsterCharacter(2, 1,
            new com.actiongame.server.domain.character.CharacterStats(50f, 5f, 2f, 3f, 0.05f, 1.5f));

        // 超高伤害
        boolean detected = detector.checkLogic(attacker, target, 50000f);
        assertTrue(detected, "Abnormal damage should trigger");
    }

    @Test
    @DisplayName("Layer2: 负伤害触发检测")
    void testNegativeDamage() {
        var stats = new com.actiongame.server.domain.character.CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        var attacker = new com.actiongame.server.domain.character.PlayerCharacter(1, 1, stats, "p1");
        var target = new com.actiongame.server.domain.character.MonsterCharacter(2, 1,
            new com.actiongame.server.domain.character.CharacterStats(50f, 5f, 2f, 3f, 0.05f, 1.5f));

        boolean detected = detector.checkLogic(attacker, target, -10f);
        assertTrue(detected, "Negative damage should trigger");
    }

    @Test
    @DisplayName("Layer2: 伤害/攻击力比率异常触发")
    void testDamageRatioAnomaly() {
        var stats = new com.actiongame.server.domain.character.CharacterStats(100f, 10f, 5f, 5f, 0.1f, 1.5f);
        var attacker = new com.actiongame.server.domain.character.PlayerCharacter(1, 1, stats, "p1");
        var target = new com.actiongame.server.domain.character.MonsterCharacter(2, 1,
            new com.actiongame.server.domain.character.CharacterStats(50f, 5f, 2f, 3f, 0.05f, 1.5f));

        // 攻击力10, 伤害200 → 20倍 (上限10倍)
        boolean detected = detector.checkLogic(attacker, target, 200f);
        assertTrue(detected, "Damage ratio anomaly should trigger");
    }

    // === 违规分数 ===

    @Test
    @DisplayName("违规分数累计")
    void testViolationScore() {
        // 触发一次 MEDIUM (score=20)
        detector.checkRealtime(1, 3, 0, 0, System.currentTimeMillis(), 5f);
        detector.checkRealtime(1, 3, 0, 0, System.currentTimeMillis() + 50, 5f);

        int score = detector.getPlayerViolationScore(1);
        assertTrue(score > 0, "Should have accumulated violation score");
    }

    @Test
    @DisplayName("无作弊时违规分数为零")
    void testNoViolationScore() {
        detector.checkRealtime(1, 1, 0.5f, 0.5f, System.currentTimeMillis(), 5f);
        assertEquals(0, detector.getPlayerViolationScore(1), "Normal behavior should have 0 score");
    }

    // === Layer 3: 回放分析 ===

    @Test
    @DisplayName("Layer3: 空回放返回0分")
    void testEmptyReplay() {
        int score = detector.analyzeReplay(null);
        assertEquals(0, score, "Null replay should return 0");
    }

    @Test
    @DisplayName("Layer3: 正常回放不触发")
    void testNormalReplay() {
        var recording = new com.actiongame.server.replay.BattleRecording("room-1", System.currentTimeMillis(), 42L, "v1");
        // 添加少量正常操作
        var frame = new com.actiongame.server.replay.BattleRecording.FrameInput(0, System.currentTimeMillis());
        frame.addAction(new com.actiongame.server.replay.BattleRecording.PlayerActionRecord(1, 3, 0, 0, 0, 0));
        recording.addFrame(frame);

        int score = detector.analyzeReplay(recording);
        assertEquals(0, score, "Normal replay should return 0");
    }
}
