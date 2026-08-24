package com.mq.novel2comic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.util.Date;
import lombok.Data;

/**
 * 分镜脚本表
 * @TableName storyboard_panel
 */
@TableName(value = "storyboard_panel", autoResultMap = true)
@Data
public class StoryboardPanel {
    /**
     * 分镜ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 小说ID
     */
    private Long novelId;

    /**
     * 分镜版本号
     */
    private Integer version;

    /**
     * 是否为当前版本：1-是，0-否
     */
    private Integer isCurrent;

    /**
     * 版本说明
     */
    private String versionNote;

    /**
     * 分镜序号
     */
    private Integer panelIndex;

    /**
     * 场景类型
     */
    private String sceneType;

    /**
     * 镜头类型
     */
    private String shotType;

    /**
     * 场景描述（中文）
     */
    private String descriptionCn;

    /**
     * 场景描述（英文）
     */
    private String descriptionEn;

    /**
     * 角色列表（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object characterList;

    /**
     * 环境描述
     */
    private String environment;

    /**
     * 情绪氛围
     */
    private String mood;

    /**
     * 对话文本
     */
    private String dialogueText;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除
     */
    private Integer isDelete;
}