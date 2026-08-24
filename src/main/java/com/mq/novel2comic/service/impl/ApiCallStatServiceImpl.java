package com.mq.novel2comic.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mq.novel2comic.model.entity.ApiCallStat;
import com.mq.novel2comic.mapper.ApiCallStatMapper;
import com.mq.novel2comic.service.ApiCallStatService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * ApiCallStat Service实现
 * @author MQ
 */
@Service
public class ApiCallStatServiceImpl extends ServiceImpl<ApiCallStatMapper, ApiCallStat>
        implements ApiCallStatService {

    @Override
    public void record(Long userId, String apiType, String modelName,
                       Integer requestTokens, Integer responseTokens, Integer imageCount,
                       BigDecimal cost, Integer responseTimeMs, Boolean success, String errorMessage) {
        ApiCallStat stat = new ApiCallStat();
        stat.setUserId(userId);
        stat.setApiType(apiType);
        stat.setModelName(modelName);
        stat.setRequestTokens(requestTokens);
        stat.setResponseTokens(responseTokens);
        stat.setImageCount(imageCount);
        stat.setCostAmount(cost);
        stat.setResponseTimeMs(responseTimeMs);
        stat.setIsSuccess(success ? 1 : 0);
        stat.setErrorMessage(errorMessage);
        stat.setCreateTime(new Date());
        this.save(stat);
    }
}
