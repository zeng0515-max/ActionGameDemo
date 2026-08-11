package com.actiongame.server.replay;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 回放存储管理器 (对应文档6.2 ReplayStorageManager)
 *
 * 回放文件存储、索引、查询、过期清理 (7天)
 * 使用JSON格式存储BattleRecording
 */
public class ReplayStorageManager {
    private static final Logger log = LoggerFactory.getLogger(ReplayStorageManager.class);
    private static final Gson gson = new Gson();

    private final Path storageDir;
    private static final long RETENTION_DAYS = 7;
    private static final String FILE_EXTENSION = ".replay.json";

    public ReplayStorageManager() {
        this(Path.of("replays"));
    }

    public ReplayStorageManager(Path storageDir) {
        this.storageDir = storageDir;
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            log.error("Failed to create replay storage directory: {}", storageDir, e);
        }
    }

    /**
     * 保存回放数据
     */
    public String save(BattleRecording recording) {
        String fileName = recording.getRoomId() + "_" + recording.getStartTimeMs() + FILE_EXTENSION;
        Path filePath = sanitizeFileName(fileName);

        try {
            String json = gson.toJson(recording);
            Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Replay saved: {}", filePath);
            return fileName;
        } catch (IOException e) {
            log.error("Failed to save replay: {}", filePath, e);
            return null;
        }
    }

    /**
     * 加载回放数据
     */
    public BattleRecording load(String fileName) {
        Path filePath = sanitizeFileName(fileName);
        if (!Files.exists(filePath)) {
            log.warn("Replay file not found: {}", filePath);
            return null;
        }

        try {
            String json = Files.readString(filePath);
            BattleRecording recording = gson.fromJson(json, BattleRecording.class);
            log.info("Replay loaded: {}", filePath);
            return recording;
        } catch (IOException e) {
            log.error("Failed to load replay: {}", filePath, e);
            return null;
        }
    }

    /**
     * 列出所有回放文件
     */
    public List<String> listReplays() {
        List<String> replays = new ArrayList<>();
        if (!Files.exists(storageDir)) return replays;

        try (Stream<Path> paths = Files.list(storageDir)) {
            paths.filter(p -> p.toString().endsWith(FILE_EXTENSION))
                 .map(p -> p.getFileName().toString())
                 .forEach(replays::add);
        } catch (IOException e) {
            log.error("Failed to list replays", e);
        }
        return replays;
    }

    /**
     * 删除指定回放
     */
    public boolean delete(String fileName) {
        Path filePath = sanitizeFileName(fileName);
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete replay: {}", filePath, e);
            return false;
        }
    }

    /**
     * 清理过期回放文件 (超过7天)
     */
    public int cleanupExpired() {
        int deleted = 0;
        Instant cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);

        if (!Files.exists(storageDir)) return 0;

        try (Stream<Path> paths = Files.list(storageDir)) {
            var expiredFiles = paths
                .filter(p -> p.toString().endsWith(FILE_EXTENSION))
                .filter(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toInstant().isBefore(cutoff);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .toList();

            for (Path file : expiredFiles) {
                try {
                    Files.delete(file);
                    deleted++;
                    log.info("Expired replay deleted: {}", file.getFileName());
                } catch (IOException e) {
                    log.warn("Failed to delete expired replay: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to cleanup expired replays", e);
        }

        if (deleted > 0) {
            log.info("Cleaned up {} expired replay files", deleted);
        }
        return deleted;
    }

    /**
     * 获取回放文件大小 (字节)
     */
    public long getFileSize(String fileName) {
        Path filePath = sanitizeFileName(fileName);
        try {
            return Files.size(filePath);
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * 校验文件名, 防止路径穿越
     */
    private Path sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Invalid fileName");
        }
        if (!fileName.matches("[a-zA-Z0-9_\\-.]+")) {
            throw new IllegalArgumentException("Invalid fileName characters: " + fileName);
        }
        if (fileName.contains("..")) {
            throw new IllegalArgumentException("Path traversal detected: " + fileName);
        }
        Path resolved = storageDir.resolve(fileName).normalize();
        if (!resolved.startsWith(storageDir.normalize())) {
            throw new IllegalArgumentException("Path escapes storage directory: " + fileName);
        }
        return resolved;
    }
}
