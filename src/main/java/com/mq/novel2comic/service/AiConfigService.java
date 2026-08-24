package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.ai.AiConfig;

/**
 * AI配置服务
 */
public interface AiConfigService {

    /**
     * 获取当前前端保存的AI配置
     */
    AiConfig getConfig();

    /**
     * 保存AI配置
     */
    AiConfig saveConfig(AiConfig config);

    /**
     * 清除前端保存的配置，恢复后端默认配置
     */
    boolean clearConfig();

    /**
     * 是否存在前端保存的配置
     */
    boolean hasStoredConfig();
}
