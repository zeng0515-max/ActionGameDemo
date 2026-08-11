package com.actiongame.server.replay;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("回放系统测试")
class ReplaySystemTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should_recordFrames_when_recorderActive")
    void should_recordFrames_when_recorderActive() {
        // Given
        BattleRecorder recorder = new BattleRecorder("room-test-01", 42L, "v1.0");

        // When
        recorder.start();
        recorder.recordFrame(0, 1000L);
        recorder.recordFrame(1, 1020L,
            new BattleRecording.PlayerActionRecord(1, 1, 0.5f, 0f, 0, -1));
        recorder.recordFrame(2, 1040L,
            new BattleRecording.PlayerActionRecord(1, 3, 0f, 0f, 0, -1));
        BattleRecording recording = recorder.stop();

        // Then
        assertThat(recording.getRoomId()).isEqualTo("room-test-01");
        assertThat(recording.getRandomSeed()).isEqualTo(42L);
        assertThat(recording.getTotalFrames()).isEqualTo(3);
        assertThat(recording.getFrames()).hasSize(3);

        // Frame 1 should have the move action
        BattleRecording.FrameInput frame1 = recording.getFrames().get(1);
        assertThat(frame1.getActions()).hasSize(1);
        assertThat(frame1.getActions().get(0).getActionType()).isEqualTo(1); // MOVE
    }

    @Test
    @DisplayName("should_saveAndLoad_when_storageManagerUsed")
    void should_saveAndLoad_when_storageManagerUsed() {
        // Given
        BattleRecorder recorder = new BattleRecorder("room-save-01", 99L, "v1.0");
        recorder.start();
        recorder.recordFrame(0, 1000L,
            new BattleRecording.PlayerActionRecord(1, 1, 1f, 0f, 0, -1));
        recorder.recordFrame(1, 1020L);
        BattleRecording recording = recorder.stop();

        ReplayStorageManager storage = new ReplayStorageManager(tempDir);

        // When
        String fileName = storage.save(recording);
        assertThat(fileName).isNotNull();

        BattleRecording loaded = storage.load(fileName);

        // Then
        assertThat(loaded).isNotNull();
        assertThat(loaded.getRoomId()).isEqualTo("room-save-01");
        assertThat(loaded.getRandomSeed()).isEqualTo(99L);
        assertThat(loaded.getTotalFrames()).isEqualTo(2);
        assertThat(loaded.getFrames()).hasSize(2);

        // Verify frame content
        assertThat(loaded.getFrames().get(0).getActions()).hasSize(1);
        assertThat(loaded.getFrames().get(0).getActions().get(0).getMoveX()).isEqualTo(1f);
    }

    @Test
    @DisplayName("should_listReplays_when_multipleSaved")
    void should_listReplays_when_multipleSaved() {
        // Given
        ReplayStorageManager storage = new ReplayStorageManager(tempDir);

        BattleRecorder r1 = new BattleRecorder("room-a", 1L, "v1.0");
        r1.start();
        r1.recordFrame(0, 1000L);
        storage.save(r1.stop());

        BattleRecorder r2 = new BattleRecorder("room-b", 2L, "v1.0");
        r2.start();
        r2.recordFrame(0, 2000L);
        storage.save(r2.stop());

        // When
        List<String> replays = storage.listReplays();

        // Then
        assertThat(replays).hasSize(2);
    }

    @Test
    @DisplayName("should_deleteReplay_when_deleteCalled")
    void should_deleteReplay_when_deleteCalled() {
        // Given
        ReplayStorageManager storage = new ReplayStorageManager(tempDir);
        BattleRecorder recorder = new BattleRecorder("room-del-01", 1L, "v1.0");
        recorder.start();
        recorder.recordFrame(0, 1000L);
        String fileName = storage.save(recorder.stop());
        assertThat(storage.listReplays()).hasSize(1);

        // When
        boolean deleted = storage.delete(fileName);

        // Then
        assertThat(deleted).isTrue();
        assertThat(storage.listReplays()).isEmpty();
    }

    @Test
    @DisplayName("should_fileSizeBeSmall_when_shortBattle")
    void should_fileSizeBeSmall_when_shortBattle() {
        // Given: 模拟10帧战斗录制
        ReplayStorageManager storage = new ReplayStorageManager(tempDir);
        BattleRecorder recorder = new BattleRecorder("room-size-01", 42L, "v1.0");

        // When: 录制10帧, 每帧1个操作
        recorder.start();
        for (int i = 0; i < 10; i++) {
            recorder.recordFrame(i, 1000L + i * 20L,
                new BattleRecording.PlayerActionRecord(1, 1, 0.5f, 0.3f, 0, -1));
        }
        String fileName = storage.save(recorder.stop());

        // Then: 文件大小应远小于500KB (验收标准)
        long size = storage.getFileSize(fileName);
        assertThat(size).isGreaterThan(0);
        assertThat(size).isLessThan(500_000L); // < 500KB
    }

    @Test
    @DisplayName("should_cleanupExpired_when_filesOlderThan7Days")
    void should_cleanupExpired_when_filesOlderThan7Days() {
        // Given: 创建一个回放文件
        ReplayStorageManager storage = new ReplayStorageManager(tempDir);
        BattleRecorder recorder = new BattleRecorder("room-expire-01", 1L, "v1.0");
        recorder.start();
        recorder.recordFrame(0, 1000L);
        storage.save(recorder.stop());
        assertThat(storage.listReplays()).hasSize(1);

        // When: 清理过期文件 (刚创建的不应被清理)
        int deleted = storage.cleanupExpired();

        // Then: 刚创建的文件不应被清理
        assertThat(deleted).isEqualTo(0);
        assertThat(storage.listReplays()).hasSize(1);
    }
}
