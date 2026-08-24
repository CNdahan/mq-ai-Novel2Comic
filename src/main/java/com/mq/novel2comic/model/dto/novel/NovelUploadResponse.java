package com.mq.novel2comic.model.dto.novel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小说上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovelUploadResponse {

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
     * 预计分镜数
     */
    private Integer estimatedPanels;

    /**
     * 预计处理时间
     */
    private String estimatedTime;

    /**
     * 提取到的角色数量
     */
    private Integer characterCount;
}
