package com.mq.novel2comic.model.dto.comic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 漫画列表项响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicListItemResponse {
    
    /**
     * 漫画ID
     */
    private Long comicId;
    
    /**
     * 漫画标题
     */
    private String title;
    
    /**
     * 小说ID
     */
    private Long novelId;
    
    /**
     * 小说标题
     */
    private String novelTitle;
    
    /**
     * 风格：japanese/chinese/realistic
     */
    private String style;
    
    /**
     * 状态：generating/completed/failed
     */
    private String status;
    
    /**
     * 分镜数量
     */
    private Integer panelCount;
    
    /**
     * 封面图（第一幅漫画）
     */
    private String coverImage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

