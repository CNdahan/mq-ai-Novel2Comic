package com.mq.novel2comic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户操作日志表
 * @TableName user_action_log
 */
@TableName(value ="user_action_log")
@Data
public class UserActionLog {
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作类型
     */
    private String actionType;

    /**
     * 资源类型
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * User Agent
     */
    private String userAgent;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}
