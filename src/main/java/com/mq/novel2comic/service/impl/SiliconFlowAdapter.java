package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.service.SiliconFlowImageClient;
import com.mq.novel2comic.service.UnifiedImageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 硅基流动适配器
 * 将硅基流动客户端适配为统一接口
 * 
 * @author MQ
 */
@Service
@ConditionalOnProperty(name = "aigc.provider", havingValue = "siliconflow")
@Slf4j
public class SiliconFlowAdapter implements UnifiedImageClient {
    
    @Autowired
    private SiliconFlowImageClient siliconFlowImageClient;
    
    @Override
    public String generateImage(String prompt, String negativePrompt, String size) {
        // 转换尺寸格式：1024*1024 -> 1024x1024
        String imageSize = size.replace("*", "x");
        return siliconFlowImageClient.generateImage(prompt, negativePrompt, imageSize);
    }
    
    @Override
    public String getProviderName() {
        return "硅基流动 (SiliconFlow)";
    }
}

