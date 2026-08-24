package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.image.ImageGenerateResult;
import com.mq.novel2comic.model.entity.StoryboardPanel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 图片生成服务
 * 负责调用AIGC接口生成漫画图片
 * 
 * @author MQ
 */
public interface ImageGenerateService {
    
    /**
     * 单个分镜异步生成
     * @param taskId 任务ID
     * @param storyboard 分镜脚本
     * @param style 风格代码
     * @return 生成结果的Future
     */
    CompletableFuture<ImageGenerateResult> generatePanelAsync(
            String taskId, 
            StoryboardPanel storyboard, 
            String style
    );
    
    /**
     * 批量并行生成
     * @param taskId 任务ID
     * @param storyboards 分镜脚本列表
     * @param style 风格代码
     * @return 生成结果列表
     */
    List<ImageGenerateResult> generateBatch(
            String taskId, 
            List<StoryboardPanel> storyboards, 
            String style
    );
}

