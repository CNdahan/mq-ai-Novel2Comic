package com.mq.novel2comic.service;

/**
 * 硅基流动图片生成客户端
 * 
 * 官网: https://siliconflow.cn
 * API文档: https://docs.siliconflow.cn/api-reference/image-generation
 * 
 * @author MQ
 */
public interface SiliconFlowImageClient {
    
    /**
     * 生成图片
     * @param prompt 正向提示词
     * @param negativePrompt 负向提示词
     * @param imageSize 图片尺寸（如 "1024x1024"）
     * @return 图片URL
     */
    String generateImage(String prompt, String negativePrompt, String imageSize);
}

