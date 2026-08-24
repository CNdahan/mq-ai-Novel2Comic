package com.mq.novel2comic;

import com.mq.novel2comic.model.dto.image.ImageGenerateResult;
import com.mq.novel2comic.model.entity.StoryboardPanel;
import com.mq.novel2comic.service.ImageGenerateService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 图片生成服务测试
 * 
 * 注意：运行此测试需要：
 * 1. 配置有效的ALIYUN_API_KEY环境变量
 * 2. 启动Redis服务
 * 3. 有足够的API调用额度
 * 
 * @author MQ
 */
@SpringBootTest
@Slf4j
public class ImageGenerateServiceTest {
    
    @Autowired
    private ImageGenerateService imageGenerateService;
    
    /**
     * 测试批量生成
     */
    @Test
    public void testGenerateBatch() {
        // 准备测试数据
        String taskId = UUID.randomUUID().toString();
        List<StoryboardPanel> storyboards = createTestStoryboards();
        
        log.info("开始测试批量生成: taskId={}, 数量={}", taskId, storyboards.size());
        
        try {
            // 批量生成
            List<ImageGenerateResult> results = imageGenerateService.generateBatch(
                    taskId,
                    storyboards,
                    "japanese"  // 日式风格
            );
            
            // 验证结果
            log.info("生成完成! 成功={}/{}", results.size(), storyboards.size());
            
            for (ImageGenerateResult result : results) {
                log.info("分镜结果: storyboardId={}, url={}, cached={}, time={}ms",
                        result.getStoryboardId(),
                        result.getImageUrl(),
                        result.getIsCached(),
                        result.getGenerateTimeMs());
            }
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }
    
    /**
     * 创建测试分镜数据
     */
    private List<StoryboardPanel> createTestStoryboards() {
        List<StoryboardPanel> storyboards = new ArrayList<>();
        
        // 分镜1：开场场景
        StoryboardPanel panel1 = new StoryboardPanel();
        panel1.setId(1L);
        panel1.setNovelId(1L);
        panel1.setPanelIndex(1);
        panel1.setSceneType("environment");
        panel1.setShotType("full");
        panel1.setDescriptionCn("清晨，阳光洒在安静的街道上");
        panel1.setDescriptionEn("Morning, sunlight shining on quiet street");
        panel1.setEnvironment("urban street, morning, peaceful atmosphere");
        panel1.setMood("calm and bright");
        storyboards.add(panel1);
        
        // 分镜2：角色特写
        StoryboardPanel panel2 = new StoryboardPanel();
        panel2.setId(2L);
        panel2.setNovelId(1L);
        panel2.setPanelIndex(2);
        panel2.setSceneType("character");
        panel2.setShotType("close_up");
        panel2.setDescriptionCn("一个年轻男子站在街角，若有所思");
        panel2.setDescriptionEn("A young man standing at the corner, looking thoughtful");
        panel2.setCharacterList("[\"李明\"]");
        panel2.setEnvironment("street corner");
        panel2.setMood("thoughtful");
        storyboards.add(panel2);
        
        // 分镜3：对话场景
        StoryboardPanel panel3 = new StoryboardPanel();
        panel3.setId(3L);
        panel3.setNovelId(1L);
        panel3.setPanelIndex(3);
        panel3.setSceneType("dialogue");
        panel3.setShotType("medium");
        panel3.setDescriptionCn("两人在咖啡厅里交谈");
        panel3.setDescriptionEn("Two people talking in a cozy coffee shop");
        panel3.setCharacterList("[\"李明\", \"王芳\"]");
        panel3.setEnvironment("cozy coffee shop, warm lighting");
        panel3.setMood("friendly and relaxed");
        panel3.setDialogueText("李明：好久不见了。\n王芳：是啊，最近怎么样？");
        storyboards.add(panel3);
        
        return storyboards;
    }
    
    /**
     * 测试单个分镜生成（需要手动运行）
     */
    // @Test
    public void testGenerateSinglePanel() {
        String taskId = UUID.randomUUID().toString();
        StoryboardPanel storyboard = createTestStoryboards().get(0);
        
        try {
            imageGenerateService.generatePanelAsync(
                    taskId,
                    storyboard,
                    "japanese"
            ).thenAccept(result -> {
                log.info("单个分镜生成完成: url={}, time={}ms",
                        result.getImageUrl(),
                        result.getGenerateTimeMs());
            }).exceptionally(ex -> {
                log.error("生成失败", ex);
                return null;
            }).join();
            
        } catch (Exception e) {
            log.error("测试失败", e);
        }
    }
}

