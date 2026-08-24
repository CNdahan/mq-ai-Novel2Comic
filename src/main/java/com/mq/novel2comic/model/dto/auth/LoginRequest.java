package com.mq.novel2comic.model.dto.auth;

import lombok.Data;

/**
 * 用户登录请求
 */
@Data
public class LoginRequest {

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码哈希
     */
    private String password;
}
