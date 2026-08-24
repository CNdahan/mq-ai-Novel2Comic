package com.mq.novel2comic.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AiConfig;
import com.mq.novel2comic.service.AiConfigService;
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
 * 文件型AI配置服务。
 * 本地部署时通过前端保存到 ./config/ai-config.json。
 */
@Slf4j
@Service
public class AiConfigServiceImpl implements AiConfigService {

    private static final Set<String> SUPPORTED_UI_PROVIDERS = Set.of("openai", "grok");

    @Value("${ai.config.file:./config/ai-config.json}")
    private String configFile;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AiConfig getConfig() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            return defaultConfig("openai");
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            AiConfig config = objectMapper.readValue(content, AiConfig.class);
            return normalize(config);
        } catch (IOException e) {
            log.error("读取AI配置失败: {}", path, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取AI配置失败");
        }
    }

    @Override
    public AiConfig saveConfig(AiConfig config) {
        AiConfig normalized = normalize(config);
        String provider = normalized.getProvider();
        if (!SUPPORTED_UI_PROVIDERS.contains(provider)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持在前端配置 GPT(OpenAI) 和 Grok");
        }
        if (normalized.getApiKey() == null || normalized.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "API Key不能为空");
        }
        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalized);
            Files.writeString(path, content, StandardCharsets.UTF_8);
            log.info("AI配置已保存: provider={}, model={}, baseUrl={}",
                    normalized.getProvider(), normalized.getModel(), normalized.getBaseUrl());
            return normalized;
        } catch (IOException e) {
            log.error("保存AI配置失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存AI配置失败");
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
            log.error("清除AI配置失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "清除AI配置失败");
        }
    }

    @Override
    public boolean hasStoredConfig() {
        return Files.exists(getConfigPath());
    }

    private AiConfig normalize(AiConfig config) {
        if (config == null) {
            return defaultConfig("openai");
        }
        String provider = normalizeProvider(config.getProvider());
        AiConfig defaults = defaultConfig(provider);
        return AiConfig.builder()
                .provider(provider)
                .apiKey(trimToEmpty(config.getApiKey()))
                .model(defaultIfBlank(config.getModel(), defaults.getModel()))
                .baseUrl(normalizeBaseUrl(defaultIfBlank(config.getBaseUrl(), defaults.getBaseUrl())))
                .build();
    }

    private AiConfig defaultConfig(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        if ("grok".equals(normalizedProvider)) {
            return AiConfig.builder()
                    .provider("grok")
                    .apiKey("")
                    .model("grok-2-latest")
                    .baseUrl("https://api.x.ai/v1")
                    .build();
        }
        return AiConfig.builder()
                .provider("openai")
                .apiKey("")
                .model("gpt-4o-mini")
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "openai";
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return "gpt".equals(normalized) ? "openai" : normalized;
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
