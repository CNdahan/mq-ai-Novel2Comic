package com.mq.novel2comic.controller;

import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.auth.*;
import com.mq.novel2comic.model.entity.User;
import java.util.Date;
import com.mq.novel2comic.service.UserService;
import com.mq.novel2comic.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Resource
    private UserService userService;
    
    @Resource
    private JwtUtils jwtUtils;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResultUtils.success(response);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResultUtils.success(response);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public BaseResponse<RefreshTokenResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        // 从Authorization头中提取token (格式: "Bearer <token>")
        String token = authHeader.replace("Bearer ", "");
        RefreshTokenResponse response = userService.refreshToken(token);
        return ResultUtils.success(response);
    }
    
    /**
     * 获取用户信息（包括剩余次数）
     * GET /auth/info
     */
    @GetMapping("/info")
    public BaseResponse<UserInfo> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        log.info("获取用户信息");
        // 从Token中提取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        // 查询用户信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        log.info("📷 从数据库读取头像: userId={}, avatar={}", userId, user.getUserAvatar());
        // 构建响应
        UserInfo userInfo = UserInfo.builder()
                .userId(user.getId())
                .username(user.getUserName())
                .email(user.getUserEmail())
                .quotaRemaining(user.getQuotaRemain())
                .quotaTotal(user.getQuotaTotal())
                .vipLevel(user.getVipLevel())
                .vipExpireAt(user.getVipExpireAt())
                .avatar(user.getUserAvatar())
                .build();
        log.info("✅ 返回用户信息: username={}, avatar={}, quotaRemaining={}", 
                user.getUserName(), user.getUserAvatar(), user.getQuotaRemain());
        return ResultUtils.success(userInfo);
    }
    
    /**
     * 更新个人信息
     * PUT /auth/profile
     */
    @PutMapping("/profile")
    public BaseResponse<Boolean> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateProfileRequest request) {
        log.info("更新个人信息请求: {}", request);
        // 从Token中提取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        // 更新个人信息
        boolean result = userService.updateProfile(userId, request);
        return ResultUtils.success(result, "个人信息更新成功");
    }
    
    /**
     * 修改密码
     * PUT /auth/password
     */
    @PutMapping("/password")
    public BaseResponse<Boolean> updatePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdatePasswordRequest request) {
        log.info("修改密码请求");
        // 从Token中提取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        // 修改密码
        boolean result = userService.updatePassword(userId, request);
        return ResultUtils.success(result, "密码修改成功");
    }
    
    /**
     * VIP升级
     * POST /auth/vip/upgrade
     */
    @PostMapping("/vip/upgrade")
    public BaseResponse<VipUpgradeResponse> upgradeVip(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody VipUpgradeRequest request) {
        log.info("VIP升级请求: {}", request);
        // 从Token中提取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        // VIP升级
        VipUpgradeResponse response = userService.upgradeVip(userId, request);
        return ResultUtils.success(response, "VIP升级成功");
    }
    
    /**
     * 用户信息VO
     */
    @lombok.Builder
    @lombok.Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String email;
        private Integer quotaRemaining;
        private Integer quotaTotal;
        private Integer vipLevel;
        private Date vipExpireAt;
        private String avatar;
    }
}
