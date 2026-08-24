package com.mq.novel2comic.model.dto.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存的图片信息
 * @author MQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedImage {
    
    /**
     * 图片URL
     */
    private String imageUrl;
    
    /**
     * 相似度（0-1）
     */
    private Double similarity;
    
    /**
     * 原始Prompt
     */
    private String originalPrompt;
    
    /**
     * 缓存时间戳
     */
    private Long timestamp;
}

