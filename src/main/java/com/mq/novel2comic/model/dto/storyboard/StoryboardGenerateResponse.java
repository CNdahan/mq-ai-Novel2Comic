package com.mq.novel2comic.model.dto.storyboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分镜生成响应
 * @author MQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardGenerateResponse {
    
    /**
     * 小说ID
     */
    private Long novelId;
    
    /**
     * 生成的分镜数量
     */
    private Integer panelCount;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 消息
     */
    private String message;
}

