package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.config.LLMConfig;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.service.AiConfigService;
import com.mq.novel2comic.service.UnifiedLLMClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * LLM路由客户端。
 * 前端保存 GPT/Grok 配置后立即生效；未保存时沿用 application.yml 的 llm.provider。
 */
@Slf4j
@Primary
@Service
public class RoutingLLMClientImpl implements UnifiedLLMClient {

    @Resource
    private AiConfigService aiConfigService;

    @Resource
    private LLMConfig llmConfig;

    @Resource
    private OpenAICompatibleLLMClientImpl openAICompatibleLLMClient;

    @Resource
    private ObjectProvider<ZhipuLLMClientImpl> zhipuLLMClientProvider;

    @Resource
    private ObjectProvider<DeepSeekLLMClientImpl> deepSeekLLMClientProvider;

    @Resource
    private ObjectProvider<DashScopeLLMClientImpl> dashScopeLLMClientProvider;

    @Override
    public String chat(String prompt, String systemPrompt) {
        UnifiedLLMClient client = resolveClient();
        log.info("🤖 使用LLM提供商: {}", client.getProviderName());
        return client.chat(prompt, systemPrompt);
    }

    @Override
    public String getProviderName() {
        return resolveClient().getProviderName();
    }

    @Override
    public boolean isAvailable() {
        return resolveClient().isAvailable();
    }

    private UnifiedLLMClient resolveClient() {
        String provider = getProvider();
        return switch (provider) {
            case "openai", "grok" -> openAICompatibleLLMClient;
            case "zhipu" -> requireClient(zhipuLLMClientProvider.getIfAvailable(), "zhipu");
            case "deepseek" -> requireClient(deepSeekLLMClientProvider.getIfAvailable(), "deepseek");
            case "dashscope" -> requireClient(dashScopeLLMClientProvider.getIfAvailable(), "dashscope");
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "不支持的LLM提供商: " + provider);
        };
    }

    private String getProvider() {
        String provider = aiConfigService.hasStoredConfig()
                ? aiConfigService.getConfig().getProvider()
                : llmConfig.getProvider();
        if (provider == null || provider.isBlank()) {
            return "zhipu";
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return "gpt".equals(normalized) ? "openai" : normalized;
    }

    private UnifiedLLMClient requireClient(UnifiedLLMClient client, String provider) {
        if (client == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "LLM提供商未启用，请检查 application.yml 的 llm.provider: " + provider);
        }
        return client;
    }
}
