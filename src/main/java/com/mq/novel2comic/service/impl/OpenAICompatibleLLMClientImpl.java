package com.mq.novel2comic.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AiConfig;
import com.mq.novel2comic.model.dto.llm.LLMRequest;
import com.mq.novel2comic.model.dto.llm.LLMResponse;
import com.mq.novel2comic.service.AiConfigService;
import com.mq.novel2comic.service.UnifiedLLMClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;

/**
 * OpenAI-compatible 客户端。
 * 支持 GPT(OpenAI) 与 Grok(xAI)，通过前端保存的 provider/baseUrl/model/apiKey 决定实际调用对象。
 */
@Slf4j
@Service
public class OpenAICompatibleLLMClientImpl implements UnifiedLLMClient {

    @Resource
    private AiConfigService aiConfigService;

    @Resource
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public String chat(String prompt, String systemPrompt) {
        AiConfig config = aiConfigService.getConfig();
        validateConfig(config);
        try {
            long startTime = System.currentTimeMillis();
            LLMRequest request = LLMRequest.builder()
                    .model(config.getModel())
                    .temperature(0.7)
                    .maxTokens(4000)
                    .messages(Arrays.asList(
                            LLMRequest.Message.builder()
                                    .role("system")
                                    .content(systemPrompt)
                                    .build(),
                            LLMRequest.Message.builder()
                                    .role("user")
                                    .content(prompt)
                                    .build()
                    ))
                    .build();
            String requestBody = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(90))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                log.error("❌ [{}] HTTP错误: {}, 响应: {}",
                        config.getProvider(), httpResponse.statusCode(), httpResponse.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        getProviderName() + "调用失败，状态码: " + httpResponse.statusCode());
            }
            LLMResponse response = objectMapper.readValue(httpResponse.body(), LLMResponse.class);
            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, getProviderName() + "返回结果为空");
            }
            String content = response.getChoices().get(0).getMessage().getContent();
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [{}] 响应成功，模型: {}, 耗时: {}ms, 响应长度: {} 字符",
                    config.getProvider(), config.getModel(), duration, content.length());
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [{}] 调用失败: {}", config.getProvider(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    getProviderName() + "调用失败: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        AiConfig config = aiConfigService.getConfig();
        if ("grok".equals(config.getProvider())) {
            return "Grok (" + config.getModel() + ")";
        }
        return "GPT/OpenAI (" + config.getModel() + ")";
    }

    @Override
    public boolean isAvailable() {
        AiConfig config = aiConfigService.getConfig();
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    private void validateConfig(AiConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, getProviderName() + " API Key未配置");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, getProviderName() + " Base URL未配置");
        }
        if (config.getModel() == null || config.getModel().isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, getProviderName() + " 模型未配置");
        }
    }
}
