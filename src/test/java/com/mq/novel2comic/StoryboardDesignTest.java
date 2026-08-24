package com.mq.novel2comic;

import com.mq.novel2comic.model.dto.novel.StoryboardPanel;
import com.mq.novel2comic.model.dto.novel.ValidationResult;
import com.mq.novel2comic.service.StoryboardService;
import com.mq.novel2comic.service.StoryboardValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分镜设计模块测试
 * 
 * @author MQ
 */
@SpringBootTest
@Slf4j
public class StoryboardDesignTest {

    @Resource
    private StoryboardValidator storyboardValidator;

    @Resource
    private StoryboardService storyboardService;

    /**
     * 测试分镜验证 - 规则1: 分镜数量
     */
    @Test
    public void testValidatePanelCount() {
        log.info("测试分镜数量验证");
        // 测试1: 分镜数量合理 (10个)
        List<StoryboardPanel> panels1 = createMockPanels(10);
        ValidationResult result1 = storyboardValidator.validatePanelCount(panels1);
        assertTrue(result1.isValid());
        assertFalse(result1.hasWarnings());
        log.info("✓ 10个分镜验证通过");
        // 测试2: 分镜数量偏少 (5个)
        List<StoryboardPanel> panels2 = createMockPanels(5);
        ValidationResult result2 = storyboardValidator.validatePanelCount(panels2);
        assertTrue(result2.isValid()); // 只是警告,不影响valid
        assertTrue(result2.hasWarnings());
        log.info("✓ 5个分镜产生警告: {}", result2.getWarnings());
        // 测试3: 分镜数量过多 (15个)
        List<StoryboardPanel> panels3 = createMockPanels(15);
        ValidationResult result3 = storyboardValidator.validatePanelCount(panels3);
        assertTrue(result3.isValid());
        assertTrue(result3.hasWarnings());
        log.info("✓ 15个分镜产生警告: {}", result3.getWarnings());
    }

    /**
     * 测试分镜验证 - 规则2: 对话场景必须包含角色
     */
    @Test
    public void testValidateDialogueScenes() {
        log.info("测试对话场景验证");
        // 测试1: 对话场景有角色
        List<StoryboardPanel> panels1 = new ArrayList<>();
        panels1.add(StoryboardPanel.builder()
                .id("1")
                .index(1)
                .sceneType("dialogue")
                .characters(Arrays.asList("李明", "王芳"))
                .build());
        ValidationResult result1 = storyboardValidator.validateDialogueScenes(panels1);
        assertTrue(result1.isValid());
        assertFalse(result1.hasErrors());
        log.info("✓ 对话场景包含角色,验证通过");
        // 测试2: 对话场景没有角色
        List<StoryboardPanel> panels2 = new ArrayList<>();
        panels2.add(StoryboardPanel.builder()
                .id("1")
                .index(1)
                .sceneType("dialogue")
                .characters(null)
                .build());
        ValidationResult result2 = storyboardValidator.validateDialogueScenes(panels2);
        assertFalse(result2.isValid());
        assertTrue(result2.hasErrors());
        log.info("✓ 对话场景缺少角色,产生错误: {}", result2.getErrors());
    }

    /**
     * 测试分镜验证 - 规则3: 避免镜头类型单一
     */
    @Test
    public void testValidateShotTypeVariety() {
        log.info("测试镜头类型多样性验证");
        // 测试1: 镜头类型多样
        List<StoryboardPanel> panels1 = new ArrayList<>();
        panels1.add(createPanel(1, "medium"));
        panels1.add(createPanel(2, "close_up"));
        panels1.add(createPanel(3, "full"));
        ValidationResult result1 = storyboardValidator.validateShotTypeVariety(panels1);
        assertTrue(result1.isValid());
        assertFalse(result1.hasWarnings());
        log.info("✓ 镜头类型多样,验证通过");
        // 测试2: 连续3个相同镜头
        List<StoryboardPanel> panels2 = new ArrayList<>();
        panels2.add(createPanel(1, "medium"));
        panels2.add(createPanel(2, "medium"));
        panels2.add(createPanel(3, "medium"));
        ValidationResult result2 = storyboardValidator.validateShotTypeVariety(panels2);
        assertTrue(result2.isValid());
        assertTrue(result2.hasWarnings());
        log.info("✓ 连续相同镜头,产生警告: {}", result2.getWarnings());
    }

    /**
     * 测试分镜验证 - 规则4: 高潮场景镜头选择
     */
    @Test
    public void testValidateClimaxScenes() {
        log.info("测试高潮场景验证");
        // 测试1: 高潮场景使用特写
        List<StoryboardPanel> panels1 = new ArrayList<>();
        panels1.add(StoryboardPanel.builder()
                .id("1")
                .index(1)
                .sceneType("climax")
                .shotType("close_up")
                .build());
        ValidationResult result1 = storyboardValidator.validateClimaxScenes(panels1);
        assertTrue(result1.isValid());
        assertFalse(result1.hasWarnings());
        log.info("✓ 高潮场景使用特写,验证通过");
        // 测试2: 高潮场景使用全景(不推荐)
        List<StoryboardPanel> panels2 = new ArrayList<>();
        panels2.add(StoryboardPanel.builder()
                .id("1")
                .index(1)
                .sceneType("climax")
                .shotType("full")
                .build());
        ValidationResult result2 = storyboardValidator.validateClimaxScenes(panels2);
        assertTrue(result2.isValid());
        assertTrue(result2.hasWarnings());
        log.info("✓ 高潮场景使用全景,产生警告: {}", result2.getWarnings());
    }

    /**
     * 测试完整验证流程
     */
    @Test
    public void testCompleteValidation() {
        log.info("测试完整验证流程");
        // 创建一个包含各种情况的分镜列表
        List<StoryboardPanel> panels = new ArrayList<>();
        // 1. 对话场景 - 有角色 - 中景
        panels.add(StoryboardPanel.builder()
                .id("1").index(1)
                .sceneType("dialogue")
                .shotType("medium")
                .characters(Arrays.asList("李明", "王芳"))
                .build());
        // 2. 动作场景 - 全景
        panels.add(StoryboardPanel.builder()
                .id("2").index(2)
                .sceneType("action")
                .shotType("full")
                .build());
        // 3. 高潮场景 - 特写(推荐)
        panels.add(StoryboardPanel.builder()
                .id("3").index(3)
                .sceneType("climax")
                .shotType("close_up")
                .build());
        // 4-10: 补充到10个分镜
        for (int i = 4; i <= 10; i++) {
            panels.add(createPanel(i, i % 2 == 0 ? "medium" : "full"));
        }
        // 执行完整验证
        ValidationResult result = storyboardValidator.validate(panels);
        assertTrue(result.isValid());
        log.info("✓ 完整验证通过");
        log.info("  - 验证消息: {}", result.getMessage());
        log.info("  - 错误数量: {}", result.getErrors().size());
        log.info("  - 警告数量: {}", result.getWarnings().size());
        if (result.hasWarnings()) {
            result.getWarnings().forEach(warning -> 
                log.warn("  警告: {}", warning)
            );
        }
    }

    /**
     * 测试Prompt生成
     */
    @Test
    public void testPromptGeneration() {
        log.info("测试Prompt生成");
        StoryboardPanel panel = StoryboardPanel.builder()
                .id("1")
                .index(1)
                .sceneType("dialogue")
                .shotType("medium")
                .descriptionCn("李明和王芳在温馨的咖啡厅对话")
                .characters(Arrays.asList("李明", "王芳"))
                .environment("温馨的咖啡厅")
                .mood("warm")
                .build();
        // 不带角色一致性
        String prompt1 = storyboardService.generatePrompt(panel, "japanese");
        assertNotNull(prompt1);
        assertTrue(prompt1.contains("Japanese manga style"));
        log.info("✓ 生成日式漫画Prompt: {}", prompt1.substring(0, Math.min(100, prompt1.length())));
        // 测试中国风格
        String prompt2 = storyboardService.generatePrompt(panel, "chinese");
        assertNotNull(prompt2);
        assertTrue(prompt2.contains("Chinese manhua style"));
        log.info("✓ 生成国风漫画Prompt: {}", prompt2.substring(0, Math.min(100, prompt2.length())));
        // 测试写实风格
        String prompt3 = storyboardService.generatePrompt(panel, "realistic");
        assertNotNull(prompt3);
        assertTrue(prompt3.contains("Semi-realistic illustration"));
        log.info("✓ 生成写实风格Prompt: {}", prompt3.substring(0, Math.min(100, prompt3.length())));
    }

    // ===== 辅助方法 =====

    /**
     * 创建模拟分镜列表
     */
    private List<StoryboardPanel> createMockPanels(int count) {
        List<StoryboardPanel> panels = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            panels.add(createPanel(i, "medium"));
        }
        return panels;
    }

    /**
     * 创建单个分镜
     */
    private StoryboardPanel createPanel(int index, String shotType) {
        return StoryboardPanel.builder()
                .id(String.valueOf(index))
                .index(index)
                .sceneType("environment")
                .shotType(shotType)
                .descriptionCn("测试场景 " + index)
                .build();
    }
}

