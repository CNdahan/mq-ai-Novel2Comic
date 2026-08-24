package com.mq.novel2comic.model.dto.auth;

import lombok.Data;

/**
 * 修改密码请求
 * @author MQ
 */
@Data
public class UpdatePasswordRequest {
    
    /**
     * 旧密码
     */
    private String oldPassword;
    
    /**
     * 新密码
     */
    private String newPassword;
    
    /**
     * 确认新密码
     */
    private String confirmPassword;
}

