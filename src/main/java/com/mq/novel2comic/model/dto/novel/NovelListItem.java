package com.mq.novel2comic.model.dto.novel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 小说列表项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovelListItem {

    /**
     * 小说ID
     */
    private Long novelId;

    /**
     * 小说标题
     */
    private String title;

    /**
     * 内容长度
     */
    private Integer contentLength;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 漫画数量
     */
    private Integer comicCount;
}
