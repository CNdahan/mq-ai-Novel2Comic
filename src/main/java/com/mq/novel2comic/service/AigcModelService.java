package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.ai.AigcConfig;

import java.util.List;

/**
 * AIGC模型发现服务
 */
public interface AigcModelService {

    List<String> listModels(AigcConfig config);
}
