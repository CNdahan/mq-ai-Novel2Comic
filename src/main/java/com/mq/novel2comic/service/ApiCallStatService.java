package com.mq.novel2comic.service;

import com.mq.novel2comic.model.entity.ApiCallStat;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;

/**
 * ApiCallStat Service
 * @author MQ
 */
public interface ApiCallStatService extends IService<ApiCallStat> {

    /**
     * 记录API调用统计
     */
    void record(Long userId, String apiType, String modelName, 
                Integer requestTokens, Integer responseTokens, Integer imageCount,
                BigDecimal cost, Integer responseTimeMs, Boolean success, String errorMessage);
}
