package com.mq.novel2comic.model.dto.novel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐小说DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NovelRecommendDTO {

    /**
     * 小说标题
     */
    private String title;

    /**
     * 小说内容
     */
    private String content;

    /**
     * 小说类型/标签
     */
    private String sourceType;

    /**
     * 字符数
     */
    private Integer characterCount;

    /**
     * 推荐理由
     */
    private String recommendation;
}

