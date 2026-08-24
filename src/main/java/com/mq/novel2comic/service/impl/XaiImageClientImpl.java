package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AigcConfig;
import com.mq.novel2comic.model.dto.image.ImageGenerationResponse;
import com.mq.novel2comic.service.AigcConfigService;
import com.mq.novel2comic.service.XaiImageClient;
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
 * xAI Grok Imagine 图片客户端
 */
@Slf4j
@Service
public class XaiImageClientImpl implements XaiImageClient {

    @Value("${aigc.grok.api-key:}")
    private String apiKey;

    @Value("${aigc.grok.model:grok-imagine-image-2.0}")
    private String defaultModel;

    @Value("${aigc.grok.base-url:https://api.x.ai/v1/images/generations}")
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
            String aspectRatio = resolveAspectRatio(normalizedSize);
            String resolution = resolveResolution(normalizedSize);
            log.info("🚀 [Grok Imagine] 开始生成图片, model={}, size={}, aspectRatio={}, resolution={}",
                    config.getModel(), normalizedSize, aspectRatio, resolution);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("prompt", fullPrompt);
            requestBody.put("n", 1);
            requestBody.put("aspect_ratio", aspectRatio);
            requestBody.put("resolution", resolution);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl()))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Grok Imagine API调用失败: statusCode={}, body={}", response.statusCode(), response.body());
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
            log.error("❌ [Grok Imagine] API调用异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "图片生成失败: " + e.getMessage() + "，请检查 AIGC Base URL 和 API Key");
        }
    }

    private AigcConfig resolveConfig() {
        AigcConfig config = aigcConfigService.getConfig();
        if ("grok".equals(config.getProvider())) {
            return config;
        }
        return AigcConfig.builder()
                .provider("grok")
                .apiKey(apiKey)
                .model(defaultModel)
                .baseUrl(defaultBaseUrl)
                .build();
    }

    private void validateConfig(AigcConfig config) {
        if (StrUtil.isBlank(config.getApiKey())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Grok 图片 API Key未配置");
        }
        if (StrUtil.isBlank(config.getModel())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Grok 图片模型未配置");
        }
        if (StrUtil.isBlank(config.getBaseUrl())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Grok 图片 Base URL未配置");
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

    private String resolveAspectRatio(String size) {
        String[] parts = size.toLowerCase().split("x");
        if (parts.length != 2) {
            return "1:1";
        }
        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            if (width == height) {
                return "1:1";
            }
            return width > height ? "16:9" : "9:16";
        } catch (Exception e) {
            return "1:1";
        }
    }

    private String resolveResolution(String size) {
        String[] parts = size.toLowerCase().split("x");
        if (parts.length != 2) {
            return "1k";
        }
        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            int max = Math.max(width, height);
            return max >= 1536 ? "2k" : "1k";
        } catch (Exception e) {
            return "1k";
        }
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
