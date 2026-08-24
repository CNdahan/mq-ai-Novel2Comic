package com.mq.novel2comic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户表
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User {
    /**
     * 用户ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 邮箱
     */
    private String userEmail;

    /**
     * 密码哈希
     */
    private String userPassword;

    /**
     * 头像URL
     */
    private String userAvatar;

    /**
     * 剩余生成次数
     */
    private Integer quotaRemain;

    /**
     * 总配额
     */
    private Integer quotaTotal;

    /**
     * VIP等级：0-免费，1-月费，2-年费
     */
    private Integer vipLevel;

    /**
     * VIP过期时间
     */
    private Date vipExpireAt;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除：0-正常，1-已删除
     */
    @TableLogic
    private Integer isDelete;
}