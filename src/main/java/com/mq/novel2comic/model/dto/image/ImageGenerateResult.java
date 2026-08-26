package com.mq.novel2comic.model.dto.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片生成结果
 * @author MQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageGenerateResult {
    
    /**
     * 分镜ID
     */
    private Long storyboardId;

    /**
     * 原始分镜序号
     */
    private Integer panelIndex;
    
    /**
     * 图片URL
     */
    private String imageUrl;
    
    /**
     * 使用的Prompt
     */
    private String prompt;
    
    /**
     * 是否缓存命中
     */
    private Boolean isCached;
    
    /**
     * 缓存相似度
     */
    private Double cacheSimilarity;
    
    /**
     * 生成耗时（毫秒）
     */
    private Integer generateTimeMs;
    
    /**
     * 图片宽度
     */
    private Integer imageWidth;
    
    /**
     * 图片高度
     */
    private Integer imageHeight;
}

