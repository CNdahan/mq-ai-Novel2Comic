package com.mq.novel2comic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 漫画面板表
 * @TableName comic_panel
 */
@TableName(value ="comic_panel")
@Data
public class ComicPanel {
    /**
     * 漫画面板ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 漫画ID
     */
    private Long comicId;

    /**
     * 小说ID
     */
    private Long novelId;

    /**
     * 分镜ID
     */
    private Long storyboardId;

    /**
     * 面板序号
     */
    private Integer panelIndex;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 宽度
     */
    private Integer imageWidth;

    /**
     * 高度
     */
    private Integer imageHeight;

    /**
     * 图片大小
     */
    private Long imageSize;

    /**
     * 风格
     */
    private String style;

    /**
     * Prompt
     */
    private String promptText;

    /**
     * 负面Prompt
     */
    private String negativePrompt;

    /** 从当前分镜提取的字幕文本，不落库。 */
    @TableField(exist = false)
    private String subtitleText;

    /**
     * 是否缓存命中
     */
    private Integer isCached;

    /**
     * 缓存相似度
     */
    private Double cacheSimilarity;

    /**
     * 生成耗时（ms）
     */
    private Integer generateTimeMs;

    /**
     * API成本
     */
    private BigDecimal apiCost;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}
