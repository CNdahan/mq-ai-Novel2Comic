package com.mq.novel2comic.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应（注册和登录）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * Token
     */
    private String token;

    /**
     * 过期时间
     */
    private Long expiresIn;

    /**
     * 剩余生成次数
     */
    private Integer quotaRemaining;
    
    /**
     * 头像URL
     */
    private String avatar;
}
