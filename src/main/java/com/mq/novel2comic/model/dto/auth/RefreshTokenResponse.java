package com.mq.novel2comic.model.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新Token响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    /**
     * 新的Token
     */
    private String token;

    /**
     * 过期时间
     */
    private Long expiresIn;
}
