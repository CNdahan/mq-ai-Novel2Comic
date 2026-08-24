package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.novel.StoryboardPanel;
import com.mq.novel2comic.model.dto.novel.ValidationResult;

import java.util.List;

/**
 * 分镜验证器接口
 * 参考需求文档功能3.4：分镜逻辑验证
 * 
 * @author MQ
 */
public interface StoryboardValidator {

    /**
     * 验证分镜设计的合理性
     * 
     * 验证规则包括：
     * 1. 单页不超过4个分镜（总数控制在8-12个）
     * 2. 对话场景必须包含角色
     * 3. 避免连续3个相同镜头类型
     * 4. 情绪高潮应该用特写或特殊角度
     * 
     * @param panels 分镜列表
     * @return 验证结果
     */
    ValidationResult validate(List<StoryboardPanel> panels);

    /**
     * 验证分镜数量是否合理
     * 
     * @param panels 分镜列表
     * @return 验证结果
     */
    ValidationResult validatePanelCount(List<StoryboardPanel> panels);

    /**
     * 验证对话场景是否包含角色
     * 
     * @param panels 分镜列表
     * @return 验证结果
     */
    ValidationResult validateDialogueScenes(List<StoryboardPanel> panels);

    /**
     * 验证镜头类型是否过于单一
     * 
     * @param panels 分镜列表
     * @return 验证结果
     */
    ValidationResult validateShotTypeVariety(List<StoryboardPanel> panels);

    /**
     * 验证高潮场景的镜头选择
     * 
     * @param panels 分镜列表
     * @return 验证结果
     */
    ValidationResult validateClimaxScenes(List<StoryboardPanel> panels);
}

