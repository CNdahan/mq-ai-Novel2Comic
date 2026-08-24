package com.mq.novel2comic.model.dto.comic;

import lombok.Data;

/**
 * 漫画生成请求
 * @author MQ
 */
@Data
public class ComicGenerateRequest {
    
    /**
     * 小说ID
     */
    private Long novelId;
    
    /**
     * 漫画风格（japanese/chinese/realistic）
     */
    private String style = "japanese";
    
    /**
     * 是否重新生成分镜（默认false，使用已有分镜）
     */
    private Boolean regenerateStoryboard = false;
    
    /**
     * 分镜版本号（可选，默认使用当前版本）
     */
    private Integer storyboardVersion;
}

