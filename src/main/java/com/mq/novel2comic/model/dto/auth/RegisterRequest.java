package com.mq.novel2comic.model.dto.auth;

import lombok.Data;

/**
 * 用户注册请求
 */
@Data
public class RegisterRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码哈希
     */
    private String password;
}
