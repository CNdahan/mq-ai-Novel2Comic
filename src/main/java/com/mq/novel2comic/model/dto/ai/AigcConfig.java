package com.mq.novel2comic.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIGC图片生成配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AigcConfig {

    /**
     * 提供商：siliconflow / wanx / openai / grok
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

    /**
     * 图片分辨率：1k / 2k
     */
    private String resolution;
}
