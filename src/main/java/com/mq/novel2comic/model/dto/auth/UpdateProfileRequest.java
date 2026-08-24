package com.mq.novel2comic.model.dto.auth;

import lombok.Data;

/**
 * 更新个人信息请求
 * @author MQ
 */
@Data
public class UpdateProfileRequest {
    
    /**
     * 用户名（可选）
     */
    private String username;
    
    /**
     * 邮箱（可选）
     */
    private String email;
    
    /**
     * 头像URL（可选）
     */
    private String avatar;
}

