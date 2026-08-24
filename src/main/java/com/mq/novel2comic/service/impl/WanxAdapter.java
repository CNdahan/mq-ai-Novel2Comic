package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.model.enums.ComicStyle;
import com.mq.novel2comic.service.UnifiedImageClient;
import com.mq.novel2comic.service.WanxImageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 通义万相适配器
 * 将通义万相客户端适配为统一接口
 * 
 * @author MQ
 */
@Service
@ConditionalOnProperty(name = "aigc.provider", havingValue = "wanx", matchIfMissing = true)
@Slf4j
public class WanxAdapter implements UnifiedImageClient {
    
    @Autowired
    private WanxImageClient wanxImageClient;
    
    @Override
    public String generateImage(String prompt, String negativePrompt, String size) {
        // 通义万相默认使用anime风格
        ComicStyle style = ComicStyle.JAPANESE;
        return wanxImageClient.generateImage(
                prompt, 
                negativePrompt, 
                style.getWanxStyle(), 
                size
        );
    }
    
    @Override
    public String getProviderName() {
        return "阿里云通义万相 (Wanx)";
    }
}

