package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.ai.AigcConfig;

/**
 * AIGC图片生成配置服务
 */
public interface AigcConfigService {

    AigcConfig getConfig();

    AigcConfig saveConfig(AigcConfig config);

    boolean clearConfig();

    boolean hasStoredConfig();
}
