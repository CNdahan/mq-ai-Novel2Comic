package com.mq.novel2comic.controller;

import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AiConfig;
import com.mq.novel2comic.service.AiConfigService;
import com.mq.novel2comic.service.AiModelService;
import com.mq.novel2comic.utils.JwtUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai/config")
public class AiConfigController {

    @Resource
    private AiConfigService aiConfigService;

    @Resource
    private AiModelService aiModelService;

    @Resource
    private JwtUtils jwtUtils;

    @GetMapping
    public BaseResponse<AiConfig> getConfig(@RequestHeader("Authorization") String authHeader) {
        validateLogin(authHeader);
        return ResultUtils.success(aiConfigService.getConfig());
    }

    @PutMapping
    public BaseResponse<AiConfig> saveConfig(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AiConfig request) {
        validateLogin(authHeader);
        AiConfig savedConfig = aiConfigService.saveConfig(request);
        return ResultUtils.success(savedConfig, "AI配置保存成功");
    }

    @DeleteMapping
    public BaseResponse<Boolean> clearConfig(@RequestHeader("Authorization") String authHeader) {
        validateLogin(authHeader);
        return ResultUtils.success(aiConfigService.clearConfig(), "AI配置已恢复默认");
    }

    @PostMapping("/models")
    public BaseResponse<java.util.List<String>> listModels(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AiConfig request) {
        validateLogin(authHeader);
        return ResultUtils.success(aiModelService.listModels(request));
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
