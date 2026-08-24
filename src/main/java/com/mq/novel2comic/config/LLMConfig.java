package com.mq.novel2comic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM配置类
 * 支持多种LLM提供商配置
 * 
 * @author MQ
 * @date 2025-10-26
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
@Data
public class LLMConfig {
    
    /**
     * 当前使用的提供商
     * 可选：zhipu, deepseek, dashscope
     */
    private String provider = "dashscope";
    
    /**
     * 智谱AI配置
     */
    private ZhipuConfig zhipu = new ZhipuConfig();
    
    /**
     * DeepSeek配置
     */
    private DeepSeekConfig deepseek = new DeepSeekConfig();
    
    @Data
    public static class ZhipuConfig {
        private String apiKey;
        private String model = "glm-4-flash";
        private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
    }
    
    @Data
    public static class DeepSeekConfig {
        private String apiKey;
        private String model = "deepseek-chat";
        private String baseUrl = "https://api.deepseek.com";
    }
}


