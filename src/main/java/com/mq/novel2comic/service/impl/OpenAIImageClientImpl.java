package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AigcConfig;
import com.mq.novel2comic.model.dto.image.ImageGenerationResponse;
import com.mq.novel2comic.service.AigcConfigService;
import com.mq.novel2comic.service.OpenAIImageClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenAI GPT Image 客户端
 */
@Slf4j
@Service
public class OpenAIImageClientImpl implements OpenAIImageClient {

    @Value("${aigc.openai.api-key:}")
    private String apiKey;

    @Value("${aigc.openai.model:gpt-image-1}")
    private String defaultModel;

    @Value("${aigc.openai.base-url:https://api.openai.com/v1/images/generations}")
    private String defaultBaseUrl;

    @Resource
    private AigcConfigService aigcConfigService;

    @Resource
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String generateImage(String prompt, String negativePrompt, String size) {
        try {
            AigcConfig config = resolveConfig();
            validateConfig(config);
            String fullPrompt = mergePrompt(prompt, negativePrompt);
            String normalizedSize = normalizeSize(size);
            log.info("🚀 [GPT Image] 开始生成图片, model={}, size={}", config.getModel(), normalizedSize);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("prompt", fullPrompt);
            requestBody.put("size", normalizedSize);
            requestBody.put("n", 1);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl()))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("GPT Image API调用失败: statusCode={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "图片生成失败: HTTP " + response.statusCode() + "，请检查 AIGC Base URL 和 API Key");
            }

            String responseBody = response.body();
            if (responseBody != null && responseBody.trim().startsWith("<")) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "图片生成失败: 接口返回了HTML页面，请检查 AIGC Base URL 是否正确");
            }

            ImageGenerationResponse imageResponse = objectMapper.readValue(responseBody, ImageGenerationResponse.class);
            return extractImageUri(imageResponse);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [GPT Image] API调用异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "图片生成失败: " + e.getMessage() + "，请检查 AIGC Base URL 和 API Key");
        }
    }

    private AigcConfig resolveConfig() {
        AigcConfig config = aigcConfigService.getConfig();
        if ("openai".equals(config.getProvider())) {
            return config;
        }
        return AigcConfig.builder()
                .provider("openai")
                .apiKey(apiKey)
                .model(defaultModel)
                .baseUrl(defaultBaseUrl)
                .build();
    }

    private void validateConfig(AigcConfig config) {
        if (StrUtil.isBlank(config.getApiKey())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "GPT Image API Key未配置");
        }
        if (StrUtil.isBlank(config.getModel())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "GPT Image模型未配置");
        }
        if (StrUtil.isBlank(config.getBaseUrl())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "GPT Image Base URL未配置");
        }
    }

    private String mergePrompt(String prompt, String negativePrompt) {
        if (StrUtil.isBlank(negativePrompt)) {
            return prompt;
        }
        return prompt + "\nAvoid: " + negativePrompt;
    }

    private String normalizeSize(String size) {
        if (StrUtil.isBlank(size)) {
            return "1024x1024";
        }
        return size.replace("*", "x");
    }

    private String extractImageUri(ImageGenerationResponse response) {
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片生成失败：未返回结果");
        }
        ImageGenerationResponse.Item item = response.getData().get(0);
        if (StrUtil.isNotBlank(item.getUrl())) {
            return item.getUrl();
        }
        if (StrUtil.isNotBlank(item.getB64Json())) {
            return "data:image/png;base64," + item.getB64Json();
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片生成失败：结果为空");
    }
}
