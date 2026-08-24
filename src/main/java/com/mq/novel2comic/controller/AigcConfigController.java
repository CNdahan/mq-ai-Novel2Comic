package com.mq.novel2comic.controller;

import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AigcConfig;
import com.mq.novel2comic.service.AigcConfigService;
import com.mq.novel2comic.service.AigcModelService;
import com.mq.novel2comic.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AIGC图片生成配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/aigc/config")
public class AigcConfigController {

    @Resource
    private AigcConfigService aigcConfigService;

    @Resource
    private AigcModelService aigcModelService;

    @Resource
    private JwtUtils jwtUtils;

    @GetMapping
    public BaseResponse<AigcConfig> getConfig(@RequestHeader("Authorization") String authHeader) {
        validateLogin(authHeader);
        return ResultUtils.success(aigcConfigService.getConfig());
    }

    @PutMapping
    public BaseResponse<AigcConfig> saveConfig(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AigcConfig request) {
        validateLogin(authHeader);
        AigcConfig savedConfig = aigcConfigService.saveConfig(request);
        return ResultUtils.success(savedConfig, "AIGC配置保存成功");
    }

    @DeleteMapping
    public BaseResponse<Boolean> clearConfig(@RequestHeader("Authorization") String authHeader) {
        validateLogin(authHeader);
        return ResultUtils.success(aigcConfigService.clearConfig(), "AIGC配置已恢复默认");
    }

    @PostMapping("/models")
    public BaseResponse<java.util.List<String>> listModels(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AigcConfig request) {
        validateLogin(authHeader);
        return ResultUtils.success(aigcModelService.listModels(request));
    }

    private void validateLogin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
    }
}
