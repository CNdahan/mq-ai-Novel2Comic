package com.mq.novel2comic.model.dto.comic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 漫画生成响应
 * @author MQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicGenerateResponse {
    
    /**
     * 漫画ID
     */
    private Long comicId;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 分镜数量
     */
    private Integer panelCount;
    
    /**
     * 预估时间
     */
    private String estimatedTime;
}

