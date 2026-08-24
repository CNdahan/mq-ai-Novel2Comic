package com.mq.novel2comic.model.dto.novel;

import com.mq.novel2comic.model.entity.CharacterProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 小说详情响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovelDetailResponse {

    /**
     * 小说ID
     */
    private Long novelId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 小说标题
     */
    private String title;

    /**
     * 小说内容
     */
    private String content;

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
     * 角色列表（完整的角色档案）
     */
    private List<CharacterProfile> characters;
}
