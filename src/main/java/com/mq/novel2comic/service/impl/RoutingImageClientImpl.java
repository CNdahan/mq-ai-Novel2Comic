package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AigcConfig;
import com.mq.novel2comic.model.enums.ComicStyle;
import com.mq.novel2comic.service.AigcConfigService;
import com.mq.novel2comic.service.OpenAIImageClient;
import com.mq.novel2comic.service.SiliconFlowImageClient;
import com.mq.novel2comic.service.UnifiedImageClient;
import com.mq.novel2comic.service.XaiImageClient;
import com.mq.novel2comic.service.WanxImageClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * AIGC图片生成路由客户端。
 * 前端保存AIGC配置后立即生效。
 */
@Slf4j
@Primary
@Service
public class RoutingImageClientImpl implements UnifiedImageClient {

    @Resource
    private AigcConfigService aigcConfigService;

    @Resource
    private SiliconFlowImageClient siliconFlowImageClient;

    @Resource
    private WanxImageClient wanxImageClient;

    @Resource
    private OpenAIImageClient openAIImageClient;

    @Resource
    private XaiImageClient xaiImageClient;

    @Override
    public String generateImage(String prompt, String negativePrompt, String size) {
        String provider = getProvider();
        log.info("🎨 使用AIGC提供商: {}", getProviderName());
        return switch (provider) {
            case "siliconflow" -> siliconFlowImageClient.generateImage(prompt, negativePrompt, size.replace("*", "x"));
            case "wanx" -> wanxImageClient.generateImage(prompt, negativePrompt, ComicStyle.JAPANESE.getWanxStyle(), size);
            case "openai" -> openAIImageClient.generateImage(prompt, negativePrompt, size);
            case "grok" -> xaiImageClient.generateImage(prompt, negativePrompt, size);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的AIGC提供商: " + provider);
        };
    }

    @Override
    public String getProviderName() {
        String provider = getProvider();
        return switch (provider) {
            case "siliconflow" -> "硅基流动 (SiliconFlow)";
            case "wanx" -> "阿里云通义万相 (Wanx)";
            case "openai" -> "GPT Image (OpenAI)";
            case "grok" -> "Grok Imagine (xAI)";
            default -> provider;
        };
    }

    private String getProvider() {
        AigcConfig config = aigcConfigService.getConfig();
        String provider = config.getProvider();
        if (provider == null || provider.isBlank()) {
            return "siliconflow";
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        if ("aliyun".equals(normalized) || "dashscope".equals(normalized)) {
            return "wanx";
        }
        if ("xai".equals(normalized)) {
            return "grok";
        }
        return normalized;
    }
}
