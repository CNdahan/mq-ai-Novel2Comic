package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.service.UnifiedLLMClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 阿里云通义千问客户端实现
 * 使用qwen-plus模型
 * 
 * 说明：
 * - 保持向后兼容，支持原有chatClientPool
 * - 响应时间较慢：2-4秒
 * - 仅在llm.provider=dashscope时启用
 * 
 * 推荐：切换到智谱AI或DeepSeek以获得4倍速提升
 * 
 * @author MQ
 * @date 2025-10-26
 */
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "dashscope", matchIfMissing = true)
@Slf4j
public class DashScopeLLMClientImpl implements UnifiedLLMClient {
    
    @Resource
    private ConcurrentHashMap<String, ChatClient> chatClientPool;
    
    // 默认使用novel_parser，可通过setCurrentClientKey切换
    private String currentClientKey = "novel_parser";
    
    /**
     * 调用阿里云通义千问进行文本生成
     */
    @Override
    public String chat(String prompt, String systemPrompt) {
        try {
            long startTime = System.currentTimeMillis();
            ChatClient client = chatClientPool.get(currentClientKey);
            if (client == null) {
                log.error("❌ [通义千问] ChatClient未配置，key: {}", currentClientKey);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "ChatClient未配置");
            }
            log.debug("🚀 [通义千问] 开始调用，clientKey: {}", currentClientKey);
            // 注意：chatClientPool中的client已经包含了systemPrompt
            // 所以这里只传user prompt
            String response = client.prompt()
                    .user(prompt)
                    .call()
                    .content();
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [通义千问] 响应成功，耗时: {}ms, 响应长度: {} 字符", 
                    duration, response.length());
            return response;
        } catch (Exception e) {
            log.error("❌ [通义千问] 调用失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                    "通义千问调用失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getProviderName() {
        return "阿里云通义千问 (qwen-plus)";
    }
    
    @Override
    public boolean isAvailable() {
        return chatClientPool != null && !chatClientPool.isEmpty();
    }
    
    /**
     * 设置当前使用的ChatClient
     * 用于切换不同任务的ChatClient（novel_parser, character_extractor等）
     */
    public void setCurrentClientKey(String key) {
        this.currentClientKey = key;
        log.debug("🔄 [通义千问] 切换ChatClient: {}", key);
    }
    
    /**
     * 获取当前ChatClient的key
     */
    public String getCurrentClientKey() {
        return currentClientKey;
    }
}


