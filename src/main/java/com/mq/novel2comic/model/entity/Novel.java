package com.mq.novel2comic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 小说表
 * @TableName novel
 */
@TableName(value ="novel")
@Data
public class Novel {

    /**
     * 小说ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 小说标题
     */
    private String novelTitle;

    /**
     * 小说内容
     */
    private String novelContent;

    /**
     * 字数
     */
    private Integer contentLength;

    /**
     * 来源类型：direct/file/url
     */
    private String sourceType;

    /**
     * 源URL
     */
    private String sourceUrl;

    /**
     * 状态：pending/processing/completed/failed
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

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
    @TableLogic
    private Integer isDelete;
}