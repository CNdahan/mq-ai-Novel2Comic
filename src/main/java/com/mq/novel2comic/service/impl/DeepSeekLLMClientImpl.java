package com.mq.novel2comic.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.config.LLMConfig;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.llm.LLMRequest;
import com.mq.novel2comic.model.dto.llm.LLMResponse;
import com.mq.novel2comic.service.UnifiedLLMClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;

/**
 * DeepSeek客户端实现
 * 使用DeepSeek-V3模型
 * 
 * 优势：
 * - 速度快：响应时间0.5-1秒
 * - 超便宜：¥0.00014/千tokens（比GLM-4更便宜）
 * - 推理强：推理能力优秀
 * 
 * 配置说明：
 * 1. 注册DeepSeek：https://platform.deepseek.com
 * 2. 获取API Key：控制台 → API密钥
 * 3. 设置环境变量：DEEPSEEK_API_KEY
 * 4. 修改配置：llm.provider: deepseek
 * 
 * @author MQ
 * @date 2025-10-26
 */
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "deepseek")
@Primary
@Slf4j
public class DeepSeekLLMClientImpl implements UnifiedLLMClient {
    
    @Autowired
    private LLMConfig llmConfig;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    /**
     * 调用DeepSeek进行文本生成
     */
    @Override
    public String chat(String prompt, String systemPrompt) {
        try {
            long startTime = System.currentTimeMillis();
            LLMConfig.DeepSeekConfig config = llmConfig.getDeepseek();
            log.debug("🚀 [DeepSeek] 开始调用，模型: {}", config.getModel());
            // 构建请求
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
            // 发送HTTP请求
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() != 200) {
                log.error("❌ [DeepSeek] HTTP错误: {}, 响应: {}", 
                        httpResponse.statusCode(), httpResponse.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                        "DeepSeek调用失败，状态码: " + httpResponse.statusCode());
            }
            // 解析响应
            LLMResponse response = objectMapper.readValue(httpResponse.body(), LLMResponse.class);
            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "DeepSeek返回结果为空");
            }
            String content = response.getChoices().get(0).getMessage().getContent();
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [DeepSeek] 响应成功，耗时: {}ms, tokens: {}, 响应长度: {} 字符", 
                    duration,
                    response.getUsage() != null ? response.getUsage().getTotalTokens() : 0,
                    content.length());
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [DeepSeek] 调用失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                    "DeepSeek调用失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getProviderName() {
        return "DeepSeek (" + llmConfig.getDeepseek().getModel() + ")";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            String response = chat("hello", "你是一个AI助手");
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            log.warn("⚠️ [DeepSeek] 服务不可用: {}", e.getMessage());
            return false;
        }
    }
}

