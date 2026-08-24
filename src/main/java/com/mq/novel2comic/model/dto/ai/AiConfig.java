package com.mq.novel2comic.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfig {

    /**
     * 提供商：openai / grok / zhipu / deepseek / dashscope
     */
    private String provider;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 接口地址
     */
    private String baseUrl;
}
