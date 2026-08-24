package com.mq.novel2comic.model.dto.comic;

import lombok.Data;

/**
 * 漫画查询请求
 */
@Data
public class ComicQueryRequest {
    
    /**
     * 页码（默认1）
     */
    private int current = 1;
    
    /**
     * 每页数量（默认10）
     */
    private int pageSize = 10;
    
    /**
     * 状态筛选：all/generating/completed/failed
     */
    private String status = "all";
    
    /**
     * 风格筛选：all/japanese/chinese/realistic
     */
    private String style = "all";
    
    /**
     * 小说ID（可选，用于筛选某个小说的所有漫画）
     */
    private Long novelId;
}

