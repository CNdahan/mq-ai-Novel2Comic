package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.ai.AiConfig;

import java.util.List;

/**
 * AI模型发现服务
 */
public interface AiModelService {

    /**
     * 获取可用模型列表
     */
    List<String> listModels(AiConfig config);
}
