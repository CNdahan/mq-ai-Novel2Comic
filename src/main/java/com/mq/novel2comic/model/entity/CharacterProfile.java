package com.mq.novel2comic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 角色档案表
 * @TableName character_profile
 */
@TableName(value = "character_profile", autoResultMap = true)
@Data
public class CharacterProfile {
    /**
     * 角色ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 小说ID
     */
    private Long novelId;

    /**
     * 角色名称
     */
    private String characterName;

    /**
     * 中文描述
     */
    private String descriptionCn;

    /**
     * 英文描述
     */
    private String descriptionEn;

    /**
     * 外貌特征数据（存储为JSON，支持List或Map结构）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object appearanceData;

    /**
     * 参考图URL
     */
    private String referenceImageUrl;

    /**
     * 特征向量（不返回给前端）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object embeddingVector;

    /**
     * 向量数据库ID
     */
    private String vectorId;

    /**
     * 使用次数
     */
    private Integer useCount;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}