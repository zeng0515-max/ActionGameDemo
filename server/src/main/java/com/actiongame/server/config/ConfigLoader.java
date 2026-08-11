package com.actiongame.server.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置加载器 - 从JSON文件加载服务端权威配置数据
 * 替代Unity ScriptableObject的运行时配置系统
 */
public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final Gson gson = new Gson();

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Path configDir;

    public ConfigLoader() {
        this(Path.of("config"));
    }

    public ConfigLoader(Path configDir) {
        this.configDir = configDir;
    }

    /**
     * 从classpath资源加载JSON配置 (打包在jar内)
     */
    public <T> T loadFromClasspath(String resourcePath, Class<T> clazz) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.error("Config resource not found: {}", resourcePath);
                return null;
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                T config = gson.fromJson(reader, clazz);
                log.info("Loaded config from classpath: {}", resourcePath);
                return config;
            }
        } catch (IOException e) {
            log.error("Failed to load config: {}", resourcePath, e);
            return null;
        }
    }

    /**
     * 从classpath资源加载JSON配置列表
     */
    public <T> List<T> loadListFromClasspath(String resourcePath, Class<T> clazz) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.error("Config resource not found: {}", resourcePath);
                return new ArrayList<>();
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type listType = TypeToken.getParameterized(List.class, clazz).getType();
                List<T> configs = gson.fromJson(reader, listType);
                log.info("Loaded {} config entries from classpath: {}", configs != null ? configs.size() : 0, resourcePath);
                return configs != null ? configs : new ArrayList<>();
            }
        } catch (IOException e) {
            log.error("Failed to load config list: {}", resourcePath, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从外部文件加载JSON配置
     */
    public <T> T loadFromFile(String fileName, Class<T> clazz) {
        Path filePath = configDir.resolve(fileName);
        if (!Files.exists(filePath)) {
            log.warn("Config file not found: {}", filePath);
            return null;
        }
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            T config = gson.fromJson(reader, clazz);
            log.info("Loaded config from file: {}", filePath);
            return config;
        } catch (IOException e) {
            log.error("Failed to load config: {}", filePath, e);
            return null;
        }
    }

    /**
     * 从外部文件加载JSON配置列表
     */
    public <T> List<T> loadListFromFile(String fileName, Class<T> clazz) {
        Path filePath = configDir.resolve(fileName);
        if (!Files.exists(filePath)) {
            log.warn("Config file not found: {}", filePath);
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            Type listType = TypeToken.getParameterized(List.class, clazz).getType();
            List<T> configs = gson.fromJson(reader, listType);
            log.info("Loaded {} config entries from file: {}", configs != null ? configs.size() : 0, filePath);
            return configs != null ? configs : new ArrayList<>();
        } catch (IOException e) {
            log.error("Failed to load config list: {}", filePath, e);
            return new ArrayList<>();
        }
    }
}
