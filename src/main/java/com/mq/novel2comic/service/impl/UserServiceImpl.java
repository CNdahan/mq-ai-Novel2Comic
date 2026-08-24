package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.exception.ThrowUtils;
import com.mq.novel2comic.model.dto.auth.*;
import com.mq.novel2comic.model.entity.User;
import com.mq.novel2comic.service.UserService;
import com.mq.novel2comic.mapper.UserMapper;
import com.mq.novel2comic.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
* @author MQ
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2025-10-20 21:11:45
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private JwtUtils jwtUtils;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Override
    public AuthResponse register(RegisterRequest request) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(request.getUsername()), ErrorCode.PARAMS_ERROR, "用户名不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getEmail()), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getPassword()), ErrorCode.PARAMS_ERROR, "密码不能为空");
        ThrowUtils.throwIf(request.getPassword().length() < 6, ErrorCode.PARAMS_ERROR, "密码长度不能少于6位");
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> usernameQuery = new LambdaQueryWrapper<>();
        usernameQuery.eq(User::getUserName, request.getUsername());
        ThrowUtils.throwIf(this.count(usernameQuery) > 0, ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        // 检查邮箱是否已存在
        LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
        emailQuery.eq(User::getUserEmail, request.getEmail());
        ThrowUtils.throwIf(this.count(emailQuery) > 0, ErrorCode.USER_ALREADY_EXISTS, "邮箱已被注册");
        // 加密密码
        String encryptedPassword = DigestUtil.md5Hex(request.getPassword());
        // 创建用户
        User user = new User();
        user.setUserName(request.getUsername());
        user.setUserEmail(request.getEmail());
        user.setUserPassword(encryptedPassword);
        user.setUserAvatar("https://raw.githubusercontent.com/lmqvq/Upload-image/main/img/202510202155825.jpg");
        user.setQuotaRemain(10);
        user.setQuotaTotal(10);
        user.setVipLevel(0);
        boolean saved = this.save(user);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "注册失败");
        // 生成Token
        String token = jwtUtils.generateToken(user.getId());
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUserName())
                .email(user.getUserEmail())
                .token(token)
                .expiresIn(jwtExpiration)
                .avatar(user.getUserAvatar())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(request.getEmail()), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getPassword()), ErrorCode.PARAMS_ERROR, "密码不能为空");
        // 查询用户
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUserEmail, request.getEmail());
        User user = this.getOne(query);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND, "用户不存在");
        // 验证密码
        String encryptedPassword = DigestUtil.md5Hex(request.getPassword());
        ThrowUtils.throwIf(!encryptedPassword.equals(user.getUserPassword()), ErrorCode.PASSWORD_ERROR, "密码错误");
        // 生成Token
        String token = jwtUtils.generateToken(user.getId());
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUserName())
                .token(token)
                .expiresIn(jwtExpiration)
                .quotaRemaining(user.getQuotaRemain())
                .avatar(user.getUserAvatar())
                .build();
    }

    @Override
    public RefreshTokenResponse refreshToken(String token) {
        // 验证Token有效性
        ThrowUtils.throwIf(!jwtUtils.validateToken(token), ErrorCode.TOKEN_EXPIRED, "Token失效");
        // 获取用户ID
        Long userId = jwtUtils.getUserIdFromToken(token);
        // 检查用户是否存在
        User user = this.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_FOUND, "用户不存在");
        // 生成新Token
        String newToken = jwtUtils.generateToken(userId);
        return RefreshTokenResponse.builder()
                .token(newToken)
                .expiresIn(jwtExpiration)
                .build();
    }
    
    @Override
    public boolean hasQuota(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            log.error("用户不存在: userId={}", userId);
            return false;
        }
        Integer quotaRemain = user.getQuotaRemain();
        boolean hasQuota = quotaRemain != null && quotaRemain > 0;
        log.info("检查用户次数: userId={}, quotaRemain={}, hasQuota={}", 
                userId, quotaRemain, hasQuota);
        return hasQuota;
    }
    
    @Override
    public boolean deductQuota(Long userId, Integer count) {
        if (count == null || count <= 0) {
            count = 1;
        }
        log.info("开始扣减用户次数: userId={}, count={}", userId, count);
        // 使用UpdateWrapper确保并发安全和原子性操作
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                     .gt("quotaRemain", 0)  // 确保剩余次数大于0
                     .setSql("quotaRemain = quotaRemain - " + count);
        boolean result = this.update(updateWrapper);
        if (result) {
            // 查询更新后的剩余次数
            User user = this.getById(userId);
            log.info("✅ 次数扣减成功: userId={}, 剩余次数={}", userId, user.getQuotaRemain());
        } else {
            log.error("❌ 次数扣减失败: userId={}, count={}", userId, count);
        }
        return result;
    }
    
    @Override
    public Integer getQuotaRemain(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            log.error("用户不存在: userId={}", userId);
            return 0;
        }
        return user.getQuotaRemain() != null ? user.getQuotaRemain() : 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("开始更新个人信息: userId={}, request={}", userId, request);
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        boolean needUpdate = false;
        // 更新用户名
        if (StrUtil.isNotBlank(request.getUsername()) && 
            !request.getUsername().equals(user.getUserName())) {
            // 检查用户名是否被占用
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUserName, request.getUsername())
                   .ne(User::getId, userId);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已被占用");
            }
            user.setUserName(request.getUsername());
            needUpdate = true;
            log.info("更新用户名: {} -> {}", user.getUserName(), request.getUsername());
        }
        // 更新邮箱
        if (StrUtil.isNotBlank(request.getEmail()) && 
            !request.getEmail().equals(user.getUserEmail())) {
            // 检查邮箱是否被占用
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUserEmail, request.getEmail())
                   .ne(User::getId, userId);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已被占用");
            }
            user.setUserEmail(request.getEmail());
            needUpdate = true;
            log.info("更新邮箱: {} -> {}", user.getUserEmail(), request.getEmail());
        }
        // 更新头像
        if (StrUtil.isNotBlank(request.getAvatar())) {
            user.setUserAvatar(request.getAvatar());
            needUpdate = true;
            log.info("更新头像URL");
        }
        if (!needUpdate) {
            log.info("没有需要更新的字段");
            return true;
        }
        boolean result = this.updateById(user);
        if (result) {
            log.info("✅ 个人信息更新成功: userId={}", userId);
        } else {
            log.error("❌ 个人信息更新失败: userId={}", userId);
        }
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(Long userId, UpdatePasswordRequest request) {
        log.info("开始修改密码: userId={}", userId);
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(request.getOldPassword()), 
                ErrorCode.PARAMS_ERROR, "旧密码不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getNewPassword()), 
                ErrorCode.PARAMS_ERROR, "新密码不能为空");
        ThrowUtils.throwIf(request.getNewPassword().length() < 6, 
                ErrorCode.PARAMS_ERROR, "新密码长度不能少于6位");
        ThrowUtils.throwIf(!request.getNewPassword().equals(request.getConfirmPassword()), 
                ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        // 查询用户
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        // 验证旧密码
        String encryptedOldPassword = DigestUtil.md5Hex(request.getOldPassword());
        if (!encryptedOldPassword.equals(user.getUserPassword())) {
            log.warn("❌ 旧密码验证失败: userId={}", userId);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "旧密码错误");
        }
        // 更新密码
        String encryptedNewPassword = DigestUtil.md5Hex(request.getNewPassword());
        user.setUserPassword(encryptedNewPassword);
        boolean result = this.updateById(user);
        if (result) {
            log.info("✅ 密码修改成功: userId={}", userId);
        } else {
            log.error("❌ 密码修改失败: userId={}", userId);
        }
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VipUpgradeResponse upgradeVip(Long userId, VipUpgradeRequest request) {
        log.info("开始VIP升级: userId={}, vipLevel={}, duration={}", 
                userId, request.getVipLevel(), request.getDuration());
        // 参数校验
        ThrowUtils.throwIf(request.getVipLevel() == null || request.getVipLevel() < 1 || request.getVipLevel() > 2,
                ErrorCode.PARAMS_ERROR, "VIP等级参数错误");
        ThrowUtils.throwIf(request.getDuration() == null || request.getDuration() < 1,
                ErrorCode.PARAMS_ERROR, "购买时长参数错误");
        // 查询用户
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        // 计算VIP过期时间
        Date now = new Date();
        LocalDateTime expireTime;
        if (user.getVipExpireAt() != null && user.getVipExpireAt().after(now)) {
            // 在当前VIP基础上延长
            expireTime = user.getVipExpireAt().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .plusMonths(request.getDuration());
        } else {
            // 从现在开始计算
            expireTime = LocalDateTime.now().plusMonths(request.getDuration());
        }
        // 计算新增配额
        int quotaAdded = calculateQuotaByVipLevel(request.getVipLevel(), request.getDuration());
        // 更新用户信息
        user.setVipLevel(request.getVipLevel());
        user.setVipExpireAt(Date.from(expireTime.atZone(ZoneId.systemDefault()).toInstant()));
        user.setQuotaRemain(user.getQuotaRemain() + quotaAdded);
        user.setQuotaTotal(user.getQuotaTotal() + quotaAdded);
        boolean result = this.updateById(user);
        if (!result) {
            log.error("❌ VIP升级失败: userId={}", userId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "VIP升级失败");
        }
        log.info("✅ VIP升级成功: userId={}, vipLevel={}, expireAt={}, quotaAdded={}", 
                userId, user.getVipLevel(), user.getVipExpireAt(), quotaAdded);
        // 生成订单ID（简化版，实际应该创建订单表）
        String orderId = "VIP_" + userId + "_" + System.currentTimeMillis();
        // 计算金额
        double amount = calculateAmount(request.getVipLevel(), request.getDuration());
        // 构建响应
        return VipUpgradeResponse.builder()
                .orderId(orderId)
                .vipLevel(user.getVipLevel())
                .vipExpireAt(user.getVipExpireAt())
                .quotaAdded(quotaAdded)
                .quotaRemaining(user.getQuotaRemain())
                .paymentStatus("completed") // Mock支付，直接完成
                .amount(amount)
                .build();
    }
    
    /**
     * 根据VIP等级和时长计算配额
     */
    private int calculateQuotaByVipLevel(Integer vipLevel, Integer duration) {
        // VIP1（月费）：每月50次
        // VIP2（年费）：每月100次
        int monthlyQuota = (vipLevel == 1) ? 50 : 100;
        return monthlyQuota * duration;
    }
    
    /**
     * 计算VIP升级金额
     */
    private double calculateAmount(Integer vipLevel, Integer duration) {
        // VIP1（月费）：9.9元/月
        // VIP2（年费）：19.9元/月
        double monthlyPrice = (vipLevel == 1) ? 9.9 : 19.9;
        return monthlyPrice * duration;
    }
}




