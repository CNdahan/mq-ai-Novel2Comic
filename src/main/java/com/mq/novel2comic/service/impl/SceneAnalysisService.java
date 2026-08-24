package com.mq.novel2comic.service.impl;

import cn.hutool.json.JSONUtil;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.novel.SceneInfo;
import com.mq.novel2comic.service.UnifiedLLMClient;
import com.mq.novel2comic.utils.LlmSystemPrompts;
import com.mq.novel2comic.utils.PromptTemplateUtils;
import com.mq.novel2comic.utils.TextCleanUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 场景分析服务
 */
@Slf4j
@Service
public class SceneAnalysisService {

    @Resource
    private UnifiedLLMClient llmClient;

    /**
     * 分析场景
     */
    public List<SceneInfo> analyze(String text) {
        // 1. 文本清洗
        String cleanedText = TextCleanUtils.clean(text);
        if (cleanedText.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文本内容为空");
        }
        // 2. 构建 Prompt
        String prompt = PromptTemplateUtils.buildSceneAnalysisPrompt(cleanedText);
        // 3. 调用 LLM 分析
        String response = callLLM(prompt);
        // 4. 解析结果
        List<SceneInfo> scenes = parseScenes(response);
        log.info("场景分析完成，共拆分 {} 个场景", scenes.size());
        return scenes;
    }

    /**
     * 调用 LLM 分析场景
     */
    private String callLLM(String prompt) {
        try {
            String response = llmClient.chat(prompt, LlmSystemPrompts.SCENE_DESIGNER);
            log.debug("场景分析LLM响应: {}", response);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用场景分析LLM失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "场景分析失败: " + e.getMessage());
        }
    }

    /**
     * 解析场景列表
     */
    private List<SceneInfo> parseScenes(String response) {
        try {
            String json = extractJson(response);
            return JSONUtil.toList(json, SceneInfo.class);
        } catch (Exception e) {
            log.error("解析场景信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析场景信息失败");
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
}
