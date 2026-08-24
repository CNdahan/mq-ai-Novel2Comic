package com.mq.novel2comic.service;

/**
 * 统一的图片生成客户端接口
 * 支持多种AIGC服务提供商
 * 
 * @author MQ
 */
public interface UnifiedImageClient {
    
    /**
     * 生成图片
     * @param prompt 正向提示词
     * @param negativePrompt 负向提示词
     * @param size 图片尺寸
     * @return 图片URL
     */
    String generateImage(String prompt, String negativePrompt, String size);
    
    /**
     * 获取提供商名称
     * @return 提供商名称（用于日志）
     */
    String getProviderName();
}

