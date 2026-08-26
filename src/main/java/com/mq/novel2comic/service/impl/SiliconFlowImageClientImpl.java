package com.mq.novel2comic.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AigcConfig;
import com.mq.novel2comic.model.dto.image.SiliconFlowRequest;
import com.mq.novel2comic.model.dto.image.SiliconFlowResponse;
import com.mq.novel2comic.service.AigcConfigService;
import com.mq.novel2comic.service.SiliconFlowImageClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 硅基流动图片生成客户端实现
 * 
 * 特点：
 * - 生成速度快：5-15秒/张
 * - 成本低：¥0.015-0.027/张
 * - 国内服务：无需代理
 * - 多种模型：SDXL、Flux等
 * 
 * 官网注册: https://siliconflow.cn
 * API文档: https://docs.siliconflow.cn/api-reference/image-generation
 * 
 * @author MQ
 */
@Service
@Primary
@Slf4j
public class SiliconFlowImageClientImpl implements SiliconFlowImageClient {
    
    @Value("${aigc.siliconflow.api-key:}")
    private String apiKey;
    
    @Value("${aigc.siliconflow.model:stabilityai/stable-diffusion-xl-base-1.0}")
    private String defaultModel;

    @Value("${aigc.siliconflow.base-url:https://api.siliconflow.cn/v1/images/generations}")
    private String defaultBaseUrl;

    @Resource
    private AigcConfigService aigcConfigService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    /**
     * 硅基流动API端点
     */
    private static final String PROVIDER = "siliconflow";
    
    /**
     * 生成图片
     * 支持重试，最多3次
     */
    @Override
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String generateImage(String prompt, String negativePrompt, String imageSize) {
        try {
            AigcConfig config = resolveConfig();
            validateConfig(config);
            String resolvedImageSize = ImageSizePolicy.sizeFor(config.getResolution());
            log.info("🚀 [硅基流动] 开始生成图片, model={}, size={}", config.getModel(), resolvedImageSize);
            long startTime = System.currentTimeMillis();
            // 1. 构建请求
            SiliconFlowRequest request = SiliconFlowRequest.builder()
                    .model(config.getModel())
                    .prompt(prompt)
                    .negativePrompt(negativePrompt)
                    .imageSize(resolvedImageSize)
                    .batchSize(1)
                    .guidanceScale(7.5)
                    .numInferenceSteps(20)  // SDXL推荐20步
                    .build();
            String requestBody = objectMapper.writeValueAsString(request);
            // 2. 发送HTTP请求
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(resolveGenerationUrl(config.getBaseUrl())))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))  // 60秒超时
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );
            // 3. 检查HTTP状态码
            if (response.statusCode() != 200) {
                log.error("硅基流动API调用失败: statusCode={}, body={}", 
                        response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                        "图片生成失败: HTTP " + response.statusCode());
            }
            // 4. 解析响应
            SiliconFlowResponse siliconFlowResponse = objectMapper.readValue(
                    response.body(),
                    SiliconFlowResponse.class
            );
            // 5. 获取图片URL
            if (siliconFlowResponse.getImages() != null 
                    && !siliconFlowResponse.getImages().isEmpty()) {
                String imageUrl = siliconFlowResponse.getImages().get(0).getUrl();
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ [硅基流动] 生成成功, url={}, 耗时={}ms", imageUrl, duration);
                return imageUrl;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片生成失败：未返回结果");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [硅基流动] API调用异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                    "图片生成失败: " + e.getMessage());
        }
    }

    private AigcConfig resolveConfig() {
        AigcConfig config = aigcConfigService.getConfig();
        if (PROVIDER.equals(config.getProvider())) {
            return config;
        }
        return AigcConfig.builder()
                .provider(PROVIDER)
                .apiKey(apiKey)
                .model(defaultModel)
                .baseUrl(defaultBaseUrl)
                .resolution(ImageSizePolicy.DEFAULT_RESOLUTION)
                .build();
    }

    private void validateConfig(AigcConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "硅基流动 API Key未配置");
        }
        if (config.getModel() == null || config.getModel().isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "硅基流动模型未配置");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "硅基流动 Base URL未配置");
        }
    }

    private String resolveGenerationUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/images/generations")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/images/generations";
        }
        return normalized + "/images/generations";
    }
}

