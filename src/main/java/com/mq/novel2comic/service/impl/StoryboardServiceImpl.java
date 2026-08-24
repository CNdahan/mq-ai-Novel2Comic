package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.novel.NovelStructure;
import com.mq.novel2comic.model.dto.novel.SceneInfo;
import com.mq.novel2comic.model.dto.novel.StoryboardPanel;
import com.mq.novel2comic.model.dto.novel.ValidationResult;
import com.mq.novel2comic.service.CharacterConsistencyService;
import com.mq.novel2comic.service.StoryboardService;
import com.mq.novel2comic.service.StoryboardValidator;
import com.mq.novel2comic.service.UnifiedLLMClient;
import com.mq.novel2comic.utils.LlmSystemPrompts;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 分镜设计服务实现
 * 参考需求文档功能3：场景分析与分镜设计
 */
@Slf4j
@Service
public class StoryboardServiceImpl implements StoryboardService {

    @Resource
    private UnifiedLLMClient llmClient;

    @Resource
    private CharacterConsistencyService characterConsistencyService;

    @Resource
    private StoryboardValidator storyboardValidator;

    /**
     * 根据小说结构生成完整分镜脚本
     */
    @Override
    public List<StoryboardPanel> generateStoryboard(NovelStructure structure) {
        if (structure == null || structure.getScenes() == null || structure.getScenes().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "小说场景信息为空");
        }
        List<StoryboardPanel> panels = new ArrayList<>();
        List<SceneInfo> scenes = structure.getScenes();
        // 为每个场景设计分镜
        for (int i = 0; i < scenes.size(); i++) {
            SceneInfo scene = scenes.get(i);
            StoryboardPanel panel = designPanel(scene, i + 1);
            panels.add(panel);
        }
        // 验证分镜合理性
        ValidationResult validationResult = storyboardValidator.validate(panels);
        if (validationResult.hasWarnings()) {
            log.warn("分镜验证存在警告，但不影响生成: {}", validationResult.getWarnings());
        }
        if (validationResult.hasErrors()) {
            log.error("分镜验证存在错误: {}", validationResult.getErrors());
        }
        log.info("分镜脚本生成完成，共 {} 个分镜", panels.size());
        return panels;
    }

    /**
     * 为单个场景设计分镜
     */
    @Override
    public StoryboardPanel designPanel(SceneInfo scene, int index) {
        // 构建分镜设计Prompt
        String prompt = buildStoryboardPrompt(scene);
        // 调用LLM设计分镜
        String response = callSceneDesigner(prompt);
        // 解析分镜设计结果
        StoryboardPanel panel = parseStoryboardPanel(response, scene, index);
        log.debug("分镜 {} 设计完成: {}", index, panel.getDescriptionCn());
        return panel;
    }

    /**
     * 验证分镜合理性
     * 参考需求文档3.4节的验证规则
     * 
     * @deprecated 使用 StoryboardValidator.validate() 替代
     */
    @Override
    @Deprecated
    public boolean validateStoryboard(List<StoryboardPanel> panels) {
        ValidationResult result = storyboardValidator.validate(panels);
        return result.isValid();
    }

    /**
     * 生成绘画Prompt
     * 参考需求文档4.3节的Prompt构建策略
     */
    @Override
    public String generatePrompt(StoryboardPanel panel, String style) {
        return generatePrompt(panel, style, null);
    }

    /**
     * 生成绘画Prompt（带角色一致性）
     * 核心方法！自动注入角色的详细描述，保证一致性
     * 参考需求文档4.3节的Prompt构建策略
     */
    @Override
    public String generatePrompt(StoryboardPanel panel, String style, Long novelId) {
        StringBuilder prompt = new StringBuilder();
        // 1. 基础场景描述
        if (panel.getDescriptionCn() != null && !panel.getDescriptionCn().isEmpty()) {
            prompt.append(panel.getDescriptionCn()).append(", ");
        }
        // 2. 角色一致性描述（核心！从向量库检索）
        if (novelId != null && panel.getCharacters() != null && !panel.getCharacters().isEmpty()) {
            for (String characterName : panel.getCharacters()) {
                try {
                    String characterDesc = characterConsistencyService.getConsistentDescription(novelId, characterName);
                    if (characterDesc != null && !characterDesc.isEmpty()) {
                        prompt.append(characterDesc).append(", ");
                        log.debug("注入角色一致性描述：{} -> {}", characterName, 
                                characterDesc.substring(0, Math.min(50, characterDesc.length())));
                    } else {
                        // 降级：只添加角色名
                        prompt.append(characterName).append(", ");
                        log.warn("角色描述为空，使用角色名：{}", characterName);
                    }
                } catch (Exception e) {
                    // 异常情况下降级
                    log.error("获取角色一致性描述失败：{}", characterName, e);
                    prompt.append(characterName).append(", ");
                }
            }
        } else if (panel.getCharacters() != null && !panel.getCharacters().isEmpty()) {
            // 没有novelId时，只添加角色名（简化版）
            prompt.append("characters: ").append(String.join(", ", panel.getCharacters())).append(", ");
        }
        // 3. 环境描述
        if (panel.getEnvironment() != null && !panel.getEnvironment().isEmpty()) {
            prompt.append("environment: ").append(panel.getEnvironment()).append(", ");
        }
        // 4. 情绪氛围
        if (panel.getMood() != null && !panel.getMood().isEmpty()) {
            String moodDesc = switch (panel.getMood()) {
                case "bright" -> "bright and cheerful atmosphere";
                case "dark" -> "dark and mysterious atmosphere";
                case "tense" -> "tense and dramatic atmosphere";
                case "warm" -> "warm and cozy atmosphere";
                default -> panel.getMood() + " atmosphere";
            };
            prompt.append(moodDesc).append(", ");
        }
        // 5. 风格控制词
        String stylePrompt = switch (style) {
            case "japanese" -> "Japanese manga style, black and white, screentone shading";
            case "chinese" -> "Chinese manhua style, colored, dynamic composition";
            case "realistic" -> "Semi-realistic illustration, detailed, cinematic lighting";
            default -> "manga style";
        };
        prompt.append(stylePrompt).append(", ");
        // 6. 质量增强词
        prompt.append("high quality, masterpiece, detailed, 8k");
        String result = prompt.toString();
        log.info("生成Prompt（长度：{}）：{}", result.length(), 
                result.substring(0, Math.min(100, result.length())) + "...");
        return result;
    }

    /**
     * 构建分镜设计Prompt
     */
    private String buildStoryboardPrompt(SceneInfo scene) {
        return String.format("""
                请为以下小说场景设计漫画分镜：
                
                场景描述：%s
                场景地点：%s
                场景时间：%s
                参与角色：%s
                
                要求：
                1. 判断场景类型：对话(dialogue)/动作(action)/环境描写(environment)/情绪转折(climax)
                2. 选择合适的镜头类型：特写(close-up)/中景(medium)/全景(full)/仰视(low-angle)/俯视(high-angle)
                3. 确定画面元素：主要角色及位置、背景环境、情绪氛围（明亮bright/阴暗dark/紧张tense/温馨warm）
                4. 生成中文场景描述和英文绘画prompt
                
                请以JSON格式返回：
                {
                  "sceneType": "dialogue",
                  "shotType": "medium",
                  "characters": ["角色1", "角色2"],
                  "environment": "咖啡厅室内",
                  "mood": "warm",
                  "descriptionCn": "中文场景描述",
                  "prompt": "English prompt for AI painting"
                }
                """,
                scene.getDescription() != null ? scene.getDescription() : scene.getContent(),
                scene.getLocation() != null ? scene.getLocation() : "未指定",
                scene.getTime() != null ? scene.getTime() : "未指定",
                scene.getCharacters() != null ? String.join(", ", scene.getCharacters()) : "无");
    }

    /**
     * 调用场景设计ChatClient
     */
    private String callSceneDesigner(String prompt) {
        try {
            String response = llmClient.chat(prompt, LlmSystemPrompts.SCENE_DESIGNER);
            log.debug("分镜设计LLM响应: {}", response);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用分镜设计LLM失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分镜设计失败: " + e.getMessage());
        }
    }

    /**
     * 解析分镜面板
     */
    private StoryboardPanel parseStoryboardPanel(String response, SceneInfo scene, int index) {
        try {
            String json = extractJson(response);
            StoryboardPanel panel = JSONUtil.toBean(json, StoryboardPanel.class);
            // 设置基本信息
            panel.setId(IdUtil.simpleUUID());
            panel.setIndex(index);
            panel.setOriginalText(scene.getContent());
            // 如果LLM没有返回某些字段，使用默认值
            if (panel.getSceneType() == null) {
                panel.setSceneType("environment");
            }
            if (panel.getShotType() == null) {
                panel.setShotType("medium");
            }
            if (panel.getCharacters() == null && scene.getCharacters() != null) {
                panel.setCharacters(scene.getCharacters());
            }
            if (panel.getEnvironment() == null && scene.getLocation() != null) {
                panel.setEnvironment(scene.getLocation());
            }
            return panel;
        } catch (Exception e) {
            log.error("解析分镜面板失败", e);
            // 返回默认分镜
            return StoryboardPanel.builder()
                    .id(IdUtil.simpleUUID())
                    .index(index)
                    .sceneType("environment")
                    .shotType("medium")
                    .characters(scene.getCharacters())
                    .environment(scene.getLocation())
                    .mood("neutral")
                    .originalText(scene.getContent())
                    .descriptionCn(scene.getDescription())
                    .build();
        }
    }

    /**
     * 提取JSON内容
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
