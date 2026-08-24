package com.mq.novel2comic.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AigcConfig;
import com.mq.novel2comic.model.dto.image.WanxRequest;
import com.mq.novel2comic.model.dto.image.WanxResponse;
import com.mq.novel2comic.service.AigcConfigService;
import com.mq.novel2comic.service.WanxImageClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 通义万相图片生成客户端实现
 * 
 * API文档: https://help.aliyun.com/zh/dashscope/developer-reference/api-details
 * 
 * @author MQ
 */
@Service
@Slf4j
public class WanxImageClientImpl implements WanxImageClient {
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.dashscope.image.wanx.model:wanx-sketch-to-image-lite}")
    private String defaultModel;

    @Value("${aigc.wanx.base-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis}")
    private String defaultBaseUrl;

    @Resource
    private AigcConfigService aigcConfigService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    /**
     * 通义万相API端点
     */
    private static final String PROVIDER = "wanx";
    
    /**
     * 生成图片（同步接口）
     * 支持重试机制，最多重试3次
     */
    @Override
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public String generateImage(String prompt, String negativePrompt, String style, String size) {
        try {
            AigcConfig config = resolveConfig();
            validateConfig(config);
            log.info("开始调用通义万相生成图片, model={}, style={}, size={}", config.getModel(), style, size);
            long startTime = System.currentTimeMillis();
            // 1. 构建请求对象
            WanxRequest request = WanxRequest.builder()
                    .model(config.getModel())
                    .input(WanxRequest.Input.builder()
                            .prompt(prompt)
                            .negativePrompt(negativePrompt)
                            .build())
                    .parameters(WanxRequest.Parameters.builder()
                            .style(style)
                            .size(size)
                            .n(1)
                            .build())
                    .build();
            // 2. 发送HTTP请求
            String requestBody = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl()))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("X-DashScope-Async", "enable") // 启用异步模式
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMinutes(5))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );
            // 3. 解析响应
            WanxResponse wanxResponse = objectMapper.readValue(
                    response.body(),
                    WanxResponse.class
            );
            // 4. 处理错误
            if (wanxResponse.getCode() != null && !wanxResponse.getCode().isEmpty()) {
                log.error("通义万相API调用失败: code={}, message={}", 
                        wanxResponse.getCode(), wanxResponse.getMessage());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                        "图片生成失败: " + wanxResponse.getMessage());
            }
            // 5. 异步任务处理
            WanxResponse.Output output = wanxResponse.getOutput();
            if (output == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片生成响应异常");
            }
            String taskId = output.getTaskId();
            if (taskId != null && !taskId.isEmpty()) {
                // 异步任务，需要轮询查询结果
                log.info("异步任务已提交: taskId={}", taskId);
                return pollTaskResult(taskId);
            }
            // 6. 同步结果直接返回
            if (output.getResults() != null && !output.getResults().isEmpty()) {
                String imageUrl = output.getResults().get(0).getUrl();
                long duration = System.currentTimeMillis() - startTime;
                log.info("图片生成成功: url={}, 耗时={}ms", imageUrl, duration);
                return imageUrl;
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片生成失败：未返回结果");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用通义万相API异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 提交异步生成任务
     */
    @Override
    public String submitTask(WanxRequest request) {
        try {
            AigcConfig config = resolveConfig();
            validateConfig(config);
            String requestBody = objectMapper.writeValueAsString(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl()))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("X-DashScope-Async", "enable")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );
            WanxResponse wanxResponse = objectMapper.readValue(
                    response.body(),
                    WanxResponse.class
            );
            if (wanxResponse.getOutput() != null) {
                return wanxResponse.getOutput().getTaskId();
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务提交失败");
        } catch (Exception e) {
            log.error("提交异步任务失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务提交失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询任务状态
     */
    @Override
    public WanxResponse queryTask(String taskId) {
        try {
            AigcConfig config = resolveConfig();
            validateConfig(config);
            String queryUrl = config.getBaseUrl() + "/" + taskId;
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(queryUrl))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );
            return objectMapper.readValue(response.body(), WanxResponse.class);
        } catch (Exception e) {
            log.error("查询任务状态失败: taskId={}", taskId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询任务失败: " + e.getMessage());
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
                .build();
    }

    private void validateConfig(AigcConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "通义万相 API Key未配置");
        }
        if (config.getModel() == null || config.getModel().isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "通义万相模型未配置");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "通义万相 Base URL未配置");
        }
    }
    
    /**
     * 轮询查询任务结果
     * 最多轮询60次，每次间隔10秒（总共10分钟）
     * 通义万相wanx-v1模型生成较慢，通常需要5-10分钟
     */
    private String pollTaskResult(String taskId) {
        int maxAttempts = 60;
        int attemptInterval = 10000; // 10秒
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(attemptInterval);
                WanxResponse response = queryTask(taskId);
                WanxResponse.Output output = response.getOutput();
                
                if (output == null) {
                    continue;
                }
                String taskStatus = output.getTaskStatus();
                log.info("任务状态: taskId={}, status={}, attempt={}/{}", 
                        taskId, taskStatus, i + 1, maxAttempts);
                if ("SUCCEEDED".equals(taskStatus)) {
                    // 任务成功
                    if (output.getResults() != null && !output.getResults().isEmpty()) {
                        String imageUrl = output.getResults().get(0).getUrl();
                        log.info("异步任务完成: taskId={}, url={}", taskId, imageUrl);
                        return imageUrl;
                    }
                } else if ("FAILED".equals(taskStatus)) {
                    // 任务失败
                    String errorMsg = output.getResults() != null && !output.getResults().isEmpty()
                            ? output.getResults().get(0).getMessage()
                            : "未知错误";
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                            "图片生成失败: " + errorMsg);
                }
                // RUNNING 或 PENDING 状态继续等待
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "任务被中断");
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("轮询任务状态异常", e);
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                "图片生成超时，请稍后重试");
    }
}

