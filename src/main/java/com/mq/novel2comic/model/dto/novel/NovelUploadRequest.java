package com.mq.novel2comic.model.dto.novel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小说上传请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NovelUploadRequest {

    /**
     * 小说标题（可选）
     */
    private String title;

    /**
     * 小说内容（必填）
     */
    private String content;

    /**
     * 来源类型：direct-直接输入, file-文件, url-URL
     */
    private String sourceType = "direct";
}
