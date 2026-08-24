package com.mq.novel2comic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 生成任务表
 * @TableName generate_task
 */
@TableName(value ="generate_task")
@Data
public class GenerateTask {
    /**
     * 任务ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务UUID
     */
    private String taskUuid;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 小说ID
     */
    private Long novelId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 状态
     */
    private String status;

    /**
     * 进度百分比
     */
    private Integer progressPercent;

    /**
     * 当前步骤
     */
    private String currentStep;

    /**
     * 总分镜数
     */
    private Integer totalPanels;

    /**
     * 已完成分镜数
     */
    private Integer completedPanels;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 完成时间
     */
    private Date completeTime;

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
