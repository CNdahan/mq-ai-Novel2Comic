package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.exception.ThrowUtils;
import com.mq.novel2comic.model.dto.novel.CharacterInfo;
import com.mq.novel2comic.model.entity.CharacterProfile;
import com.mq.novel2comic.service.CharacterConsistencyService;
import com.mq.novel2comic.service.CharacterExtractService;
import com.mq.novel2comic.service.UnifiedLLMClient;
import com.mq.novel2comic.utils.LlmSystemPrompts;
import com.mq.novel2comic.utils.PromptTemplateUtils;
import com.mq.novel2comic.utils.TextCleanUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色提取服务实现
 */
@Slf4j
@Service
public class CharacterExtractServiceImpl implements CharacterExtractService {

    @Resource
    private UnifiedLLMClient llmClient;

    @Resource
    private CharacterConsistencyService characterConsistencyService;

    @Override
    public List<CharacterInfo> extract(String text) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(text), ErrorCode.PARAMS_ERROR, "文本内容不能为空");
        // 文本清洗
        String cleanedText = TextCleanUtils.clean(text);
        ThrowUtils.throwIf(cleanedText.isEmpty(), ErrorCode.PARAMS_ERROR, "文本内容为空");
        // 构建 Prompt
        String prompt = PromptTemplateUtils.buildCharacterExtractPrompt(cleanedText);
        // 调用 LLM 分析
        String response = callLLM(prompt);
        // 解析结果
        List<CharacterInfo> characters = parseCharacters(response);
        log.info("角色提取完成，共提取 {} 个角色", characters.size());
        return characters;
    }

    /**
     * 调用 LLM 提取角色
     */
    private String callLLM(String prompt) {
        try {
            String response = llmClient.chat(prompt, LlmSystemPrompts.CHARACTER_EXTRACTOR);
            log.debug("角色提取LLM响应: {}", response);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用角色提取LLM失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "角色提取失败: " + e.getMessage());
        }
    }

    /**
     * 解析角色列表
     */
    private List<CharacterInfo> parseCharacters(String response) {
        try {
            String json = extractJson(response);
            return JSONUtil.toList(json, CharacterInfo.class);
        } catch (Exception e) {
            log.error("解析角色信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析角色信息失败");
        }
    }

    /**
     * 提取 JSON 内容
     */
    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    @Override
    public List<CharacterProfile> extractAndStore(Long novelId, String text) {
        log.info("开始提取角色并存储：novelId={}", novelId);
        try {
            // 1. 提取角色信息
            List<CharacterInfo> characters = extract(text);
            if (characters.isEmpty()) {
                log.warn("未提取到任何角色：novelId={}", novelId);
                return List.of();
            }
            // 2. 批量存储并向量化
            List<CharacterProfile> profiles = characterConsistencyService.storeCharacters(novelId, characters);

            log.info("角色提取并存储完成：novelId={}, 成功{}/{}", 
                    novelId, profiles.size(), characters.size());

            return profiles;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提取并存储角色失败：novelId={}", novelId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提取并存储角色失败: " + e.getMessage());
        }
    }
}
