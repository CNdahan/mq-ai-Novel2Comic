package com.mq.novel2comic.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.model.entity.CharacterProfile;
import com.mq.novel2comic.model.entity.StoryboardPanel;
import com.mq.novel2comic.model.enums.ComicStyle;
import com.mq.novel2comic.model.enums.ShotType;
import com.mq.novel2comic.service.CharacterProfileService;
import com.mq.novel2comic.service.PromptBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Prompt构建服务实现
 * 
 * 构建策略：
 * 最终Prompt = 基础场景描述 + 角色一致性描述 + 镜头类型 + 风格控制 + 质量增强
 * 
 * @author MQ
 */
@Service
@Slf4j
public class PromptBuilderServiceImpl implements PromptBuilderService {
    
    @Autowired
    private CharacterProfileService characterProfileService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 构建最终的生成Prompt
     */
    @Override
    public String buildFinalPrompt(StoryboardPanel storyboard, String style) {
        StringBuilder prompt = new StringBuilder();
        
        // 1. 基础场景描述（来自分镜设计）
        String baseDescription = storyboard.getDescriptionEn();
        if (baseDescription != null && !baseDescription.isEmpty()) {
            prompt.append(baseDescription);
        } else {
            // 降级使用中文描述（API会自动翻译）
            prompt.append(storyboard.getDescriptionCn());
        }
        prompt.append(", ");
        
        // 2. 镜头类型描述
        String shotType = storyboard.getShotType();
        if (shotType != null && !shotType.isEmpty()) {
            ShotType shot = ShotType.fromCode(shotType);
            prompt.append(shot.getPromptDesc()).append(", ");
        }
        
        // 3. 角色一致性描述（从角色档案获取）
        Object characterListObj = storyboard.getCharacterList();
        if (characterListObj != null) {
            try {
                // JacksonTypeHandler 已经将 JSON 反序列化为 List 对象，直接使用
                List<String> characterNames;
                if (characterListObj instanceof List) {
                    // 直接类型转换
                    characterNames = (List<String>) characterListObj;
                } else {
                    // 降级处理：使用 ObjectMapper 转换
                    characterNames = objectMapper.convertValue(
                            characterListObj,
                            new TypeReference<List<String>>() {}
                    );
                }
                
                for (String characterName : characterNames) {
                    CharacterProfile profile = characterProfileService.getByNovelIdAndName(
                            storyboard.getNovelId(), 
                            characterName
                    );
                    
                    if (profile != null && profile.getDescriptionEn() != null) {
                        prompt.append(profile.getDescriptionEn()).append(", ");
                    }
                }
            } catch (Exception e) {
                log.warn("解析角色列表失败，将跳过角色描述: {}", characterListObj, e);
            }
        }
        
        // 4. 环境描述
        String environment = storyboard.getEnvironment();
        if (environment != null && !environment.isEmpty()) {
            prompt.append(environment).append(", ");
        }
        
        // 5. 情绪氛围
        String mood = storyboard.getMood();
        if (mood != null && !mood.isEmpty()) {
            prompt.append(mood).append(" atmosphere, ");
        }
        
        // 6. 风格控制词
        ComicStyle comicStyle = ComicStyle.fromCode(style);
        prompt.append(comicStyle.getStylePrompt()).append(", ");
        
        // 7. 质量增强词
        prompt.append("high quality, masterpiece, detailed, professional illustration");
        
        String finalPrompt = prompt.toString();
        log.debug("构建Prompt完成: {}", finalPrompt);
        
        return finalPrompt;
    }
    
    /**
     * 构建负面Prompt
     * 避免生成质量差、变形、水印等问题
     */
    @Override
    public String buildNegativePrompt() {
        return "blurry, ugly, bad anatomy, distorted, deformed, disfigured, " +
               "poorly drawn, bad proportions, gross proportions, " +
               "watermark, signature, text, error, cropped, " +
               "low quality, worst quality, jpeg artifacts, duplicate";
    }
}

