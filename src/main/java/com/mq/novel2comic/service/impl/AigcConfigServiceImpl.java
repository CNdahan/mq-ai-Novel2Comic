package com.mq.novel2comic.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AigcConfig;
import com.mq.novel2comic.service.AigcConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

/**
 * 文件型AIGC配置服务。
 * 本地部署时通过前端保存到 ./config/aigc-config.json。
 */
@Slf4j
@Service
public class AigcConfigServiceImpl implements AigcConfigService {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("siliconflow", "wanx", "openai", "grok");

    @Value("${aigc.config.file:./config/aigc-config.json}")
    private String configFile;

    @Value("${aigc.provider:siliconflow}")
    private String defaultProvider;

    @Value("${aigc.resolution:1k}")
    private String defaultResolution;

    @Value("${aigc.siliconflow.api-key:}")
    private String siliconFlowApiKey;

    @Value("${aigc.siliconflow.model:black-forest-labs/FLUX.1-schnell}")
    private String siliconFlowModel;

    @Value("${spring.ai.dashscope.api-key:}")
    private String wanxApiKey;

    @Value("${spring.ai.dashscope.image.wanx.model:wanx-v1}")
    private String wanxModel;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AigcConfig getConfig() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            return defaultConfig(defaultProvider);
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            AigcConfig config = objectMapper.readValue(content, AigcConfig.class);
            return normalize(config);
        } catch (IOException e) {
            log.error("读取AIGC配置失败: {}", path, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取AIGC配置失败");
        }
    }

    @Override
    public AigcConfig saveConfig(AigcConfig config) {
        AigcConfig normalized = normalize(config);
        if (!SUPPORTED_PROVIDERS.contains(normalized.getProvider())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 SiliconFlow、通义万相、GPT Image 和 Grok");
        }
        if (normalized.getApiKey() == null || normalized.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AIGC API Key不能为空");
        }
        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalized);
            Files.writeString(path, content, StandardCharsets.UTF_8);
            log.info("AIGC配置已保存: provider={}, model={}, baseUrl={}",
                    normalized.getProvider(), normalized.getModel(), normalized.getBaseUrl());
            return normalized;
        } catch (IOException e) {
            log.error("保存AIGC配置失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存AIGC配置失败");
        }
    }

    @Override
    public boolean clearConfig() {
        try {
            Path path = getConfigPath();
            if (Files.exists(path)) {
                Files.delete(path);
            }
            return true;
        } catch (IOException e) {
            log.error("清除AIGC配置失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "清除AIGC配置失败");
        }
    }

    @Override
    public boolean hasStoredConfig() {
        return Files.exists(getConfigPath());
    }

    private AigcConfig normalize(AigcConfig config) {
        if (config == null) {
            return defaultConfig(defaultProvider);
        }
        String provider = normalizeProvider(config.getProvider());
        AigcConfig defaults = defaultConfig(provider);
        return AigcConfig.builder()
                .provider(provider)
                .apiKey(defaultIfBlank(config.getApiKey(), defaults.getApiKey()))
                .model(defaultIfBlank(config.getModel(), defaults.getModel()))
                .baseUrl(normalizeBaseUrl(defaultIfBlank(config.getBaseUrl(), defaults.getBaseUrl())))
                .resolution(ImageSizePolicy.normalizeResolution(
                        defaultIfBlank(config.getResolution(), defaults.getResolution())))
                .build();
    }

    private AigcConfig defaultConfig(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        if ("wanx".equals(normalizedProvider)) {
            return AigcConfig.builder()
                    .provider("wanx")
                    .apiKey(trimToEmpty(wanxApiKey))
                    .model(defaultIfBlank(wanxModel, "wanx-v1"))
                    .baseUrl("https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis")
                    .resolution(getDefaultResolution())
                    .build();
        }
        if ("openai".equals(normalizedProvider)) {
            return AigcConfig.builder()
                    .provider("openai")
                    .apiKey("")
                    .model("gpt-image-1")
                    .baseUrl("https://api.openai.com/v1/images/generations")
                    .resolution(getDefaultResolution())
                    .build();
        }
        if ("grok".equals(normalizedProvider)) {
            return AigcConfig.builder()
                    .provider("grok")
                    .apiKey("")
                    .model("grok-imagine-image-2.0")
                    .baseUrl("https://api.x.ai/v1/images/generations")
                    .resolution(getDefaultResolution())
                    .build();
        }
        return AigcConfig.builder()
                .provider("siliconflow")
                .apiKey(trimToEmpty(siliconFlowApiKey))
                .model(defaultIfBlank(siliconFlowModel, "black-forest-labs/FLUX.1-schnell"))
                .baseUrl("https://api.siliconflow.cn/v1/images/generations")
                .resolution(getDefaultResolution())
                .build();
    }

    private String getDefaultResolution() {
        return ImageSizePolicy.normalizeResolution(defaultResolution);
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            provider = defaultProvider;
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        if ("aliyun".equals(normalized) || "dashscope".equals(normalized)) {
            return "wanx";
        }
        if ("gpt".equals(normalized) || "gpt-image".equals(normalized) || "gpt-image-1".equals(normalized)) {
            return "openai";
        }
        if ("xai".equals(normalized) || "grok-image".equals(normalized)) {
            return "grok";
        }
        return normalized;
    }

    private Path getConfigPath() {
        return Paths.get(configFile).toAbsolutePath().normalize();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = value.trim();
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        } else if (normalized.startsWith("\"") || normalized.startsWith("'")) {
            normalized = normalized.substring(1).trim();
        } else if (normalized.endsWith("\"") || normalized.endsWith("'")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized.endsWith("/") && normalized.length() > 1
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }
}
