package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.auth.*;
import com.mq.novel2comic.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author MQ
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2025-10-20 21:11:45
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 用户登录
     */
    AuthResponse login(LoginRequest request);

    /**
     * 刷新Token
     */
    RefreshTokenResponse refreshToken(String token);
    
    /**
     * 检查用户剩余次数是否足够
     * 
     * @param userId 用户ID
     * @return 是否有剩余次数
     */
    boolean hasQuota(Long userId);
    
    /**
     * 扣减用户生成次数
     * 
     * @param userId 用户ID
     * @param count 扣减数量（默认1）
     * @return 是否扣减成功
     */
    boolean deductQuota(Long userId, Integer count);
    
    /**
     * 获取用户剩余次数
     * 
     * @param userId 用户ID
     * @return 剩余次数
     */
    Integer getQuotaRemain(Long userId);
    
    /**
     * 更新个人信息
     * 
     * @param userId 用户ID
     * @param request 更新请求
     * @return 是否成功
     */
    boolean updateProfile(Long userId, UpdateProfileRequest request);
    
    /**
     * 修改密码
     * 
     * @param userId 用户ID
     * @param request 修改密码请求
     * @return 是否成功
     */
    boolean updatePassword(Long userId, UpdatePasswordRequest request);
    
    /**
     * VIP升级
     * 
     * @param userId 用户ID
     * @param request VIP升级请求
     * @return VIP升级响应
     */
    VipUpgradeResponse upgradeVip(Long userId, VipUpgradeRequest request);
}
