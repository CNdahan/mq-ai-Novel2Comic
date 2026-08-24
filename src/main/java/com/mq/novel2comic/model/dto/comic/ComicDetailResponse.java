package com.mq.novel2comic.model.dto.comic;

import com.mq.novel2comic.model.entity.ComicPanel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 漫画详情响应
 * @author MQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComicDetailResponse {
    
    /**
     * 漫画ID
     */
    private Long comicId;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 风格
     */
    private String style;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 面板数量
     */
    private Integer panelCount;
    
    /**
     * 面板列表
     */
    private List<ComicPanel> panels;
}

