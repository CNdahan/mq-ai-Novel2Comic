package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.image.CachedImage;

import java.util.Optional;

/**
 * 语义缓存服务
 * 使用向量相似度检测语义相近的Prompt，复用已生成的图片
 * 
 * @author MQ
 */
public interface SemanticCacheService {
    
    /**
     * 检查语义缓存
     * @param prompt 待检查的Prompt
     * @return 缓存的图片信息（如果命中）
     */
    Optional<CachedImage> checkCache(String prompt, String cacheScope);
    
    /**
     * 缓存新生成的图片
     * @param prompt 使用的Prompt
     * @param imageUrl 图片URL
     */
    void cacheImage(String prompt, String imageUrl, String cacheScope);
    
    /**
     * 清除过期缓存
     */
    void clearExpiredCache();
}

