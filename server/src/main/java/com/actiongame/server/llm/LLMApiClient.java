package com.actiongame.server.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * LLM API HTTP 客户端 — 为未来接入真实 LLM API 预留
 *
 * 支持的 LLM 后端 (配置切换):
 * - OpenAI GPT API
 * - 火山引擎豆包 API
 * - 通义千问 API
 *
 * 当前实现为 stub, 返回空结果, 实际使用时替换为真实 API 调用
 */
public class LLMApiClient {

    private final HttpClient httpClient;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public LLMApiClient(String apiUrl, String apiKey, String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * 发送 prompt 到 LLM API (异步)
     * @param prompt 输入文本
     * @return LLM 响应文本 (CompletableFuture)
     */
    public CompletableFuture<String> sendPrompt(String prompt) {
        if (apiUrl == null || apiUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            // 未配置 API, 返回空
            return CompletableFuture.completedFuture("");
        }

        try {
            String requestBody = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"max_tokens\":200}",
                model, escapeJson(prompt)
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(10))
                .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(e -> {
                    System.err.println("[LLMApiClient] API call failed: " + e.getMessage());
                    return "";
                });
        } catch (Exception e) {
            return CompletableFuture.completedFuture("");
        }
    }

    /**
     * 检查 API 是否可用
     */
    public boolean isAvailable() {
        return apiUrl != null && !apiUrl.isEmpty() && apiKey != null && !apiKey.isEmpty();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
