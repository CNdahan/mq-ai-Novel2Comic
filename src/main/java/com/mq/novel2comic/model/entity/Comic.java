package com.mq.novel2comic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 漫画作品表
 * @TableName comic
 */
@TableName(value ="comic")
@Data
public class Comic {
    /**
     * 漫画ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 小说ID
     */
    private Long novelId;

    /**
     * 漫画标题
     */
    private String comicTitle;

    /**
     * 封面URL
     */
    private String coverUrl;

    /**
     * 分镜数量
     */
    private Integer panelCount;

    /**
     * 风格
     */
    private String style;

    /**
     * 总成本
     */
    private BigDecimal totalCost;

    /**
     * 总耗时
     */
    private Integer totalTimeMs;

    /**
     * 缓存命中率
     */
    private Double cacheHitRate;

    /**
     * 状态
     */
    private String status;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 点赞次数
     */
    private Integer likeCount;

    /**
     * 分享次数
     */
    private Integer shareCount;

    /**
     * 是否公开
     */
    private Integer isPublic;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}