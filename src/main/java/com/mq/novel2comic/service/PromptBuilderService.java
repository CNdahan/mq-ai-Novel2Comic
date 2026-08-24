package com.mq.novel2comic.service;

import com.mq.novel2comic.model.entity.StoryboardPanel;

/**
 * Prompt构建服务
 * @author MQ
 */
public interface PromptBuilderService {
    
    /**
     * 构建最终的生成Prompt
     * @param storyboard 分镜脚本
     * @param style 风格代码
     * @return 完整的Prompt
     */
    String buildFinalPrompt(StoryboardPanel storyboard, String style);
    
    /**
     * 构建负面Prompt
     * @return 负面Prompt
     */
    String buildNegativePrompt();
}

