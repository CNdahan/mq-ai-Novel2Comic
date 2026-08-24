package com.mq.novel2comic.model.dto.storyboard;

import lombok.Data;

import java.util.List;

/**
 * 更新分镜请求
 * 
 * @author MQ
 */
@Data
public class UpdateStoryboardRequest {
    
    /**
     * 场景类型
     */
    private String sceneType;
    
    /**
     * 镜头类型
     */
    private String shotType;
    
    /**
     * 场景描述（中文）
     */
    private String descriptionCn;
    
    /**
     * 场景描述（英文）
     */
    private String descriptionEn;
    
    /**
     * 角色列表
     */
    private List<String> characters;
    
    /**
     * 环境描述
     */
    private String environment;
    
    /**
     * 情绪氛围
     */
    private String mood;
    
    /**
     * 对话文本
     */
    private String dialogueText;
}

