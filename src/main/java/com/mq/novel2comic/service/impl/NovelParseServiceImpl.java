package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.exception.ThrowUtils;
import com.mq.novel2comic.model.dto.novel.NovelStructure;
import com.mq.novel2comic.service.NovelParseService;
import com.mq.novel2comic.service.UnifiedLLMClient;
import com.mq.novel2comic.utils.LlmSystemPrompts;
import com.mq.novel2comic.utils.PromptTemplateUtils;
import com.mq.novel2comic.utils.TextCleanUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 小说文本解析服务实现
 * 参考需求文档功能1：小说文本输入与解析
 */
@Slf4j
@Service
public class NovelParseServiceImpl implements NovelParseService {

    @Resource
    private UnifiedLLMClient llmClient;

    /**
     * 解析小说文本结构
     */
    @Override
    public NovelStructure parse(String text) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(text), ErrorCode.PARAMS_ERROR, "文本内容不能为空");
        // 1. 基础清洗
        String cleanedText = cleanText(text);
        ThrowUtils.throwIf(cleanedText.isEmpty(), ErrorCode.PARAMS_ERROR, "文本内容为空");
        // 2. LLM分析文本结构
        String prompt = PromptTemplateUtils.buildParsePrompt(cleanedText);
        String response = callLLM(prompt);
        // 3. 解析LLM返回的JSON
        NovelStructure structure = parseStructure(response);
        log.info("小说解析完成，标题: {}, 角色数: {}, 场景数: {}",
                structure.getTitle(),
                structure.getCharacters() != null ? structure.getCharacters().size() : 0,
                structure.getScenes() != null ? structure.getScenes().size() : 0);
        return structure;
    }

    /**
     * 清洗文本
     */
    @Override
    public String cleanText(String text) {
        return TextCleanUtils.clean(text);
    }

    /**
     * 段落分割（按换行符）
     */
    @Override
    public List<String> splitParagraphs(String text) {
        if (StrUtil.isBlank(text)) {
            return new ArrayList<>();
        }
        return Arrays.stream(text.split("\n+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 识别对话（引号内容）
     */
    @Override
    public List<String> extractDialogues(String text) {
        if (StrUtil.isBlank(text)) {
            return new ArrayList<>();
        }
        List<String> dialogues = new ArrayList<>();
        // 匹配中文引号和英文引号
        Pattern pattern = Pattern.compile("[\"“]([^\"”]+)[\"”]");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String dialogue = matcher.group(1).trim();
            if (!dialogue.isEmpty()) {
                dialogues.add(dialogue);
            }
        }
        log.debug("识别到 {} 段对话", dialogues.size());
        return dialogues;
    }

    /**
     * 调用 LLM 分析
     */
    private String callLLM(String prompt) {
        try {
            String response = llmClient.chat(prompt, LlmSystemPrompts.NOVEL_PARSER);
            log.debug("小说解析LLM响应: {}", response);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用小说解析LLM失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "小说解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析结构化数据
     */
    private NovelStructure parseStructure(String response) {
        try {
            String json = extractJson(response);
            return JSONUtil.toBean(json, NovelStructure.class);
        } catch (Exception e) {
            log.error("解析小说结构失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析小说结构失败");
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
