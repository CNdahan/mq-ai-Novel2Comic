package com.mq.novel2comic.model.dto.storyboard;

import com.mq.novel2comic.model.dto.novel.ValidationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分镜响应
 * 
 * @author MQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardResponse {
    
    /**
     * 小说ID
     */
    private Long novelId;
    
    /**
     * 分镜列表
     */
    private List<StoryboardPanelVO> panels;
    
    /**
     * 验证结果
     */
    private ValidationResult validationResult;
    
    /**
     * 总分镜数
     */
    private Integer totalCount;
    
    /**
     * 预估生成时间（分钟）
     */
    private String estimatedTime;
    
    /**
     * 分镜面板VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoryboardPanelVO {
        /**
         * 分镜ID
         */
        private String id;
        
        /**
         * 分镜序号
         */
        private Integer index;
        
        /**
         * 场景类型
         */
        private String sceneType;
        
        /**
         * 场景类型描述
         */
        private String sceneTypeDesc;
        
        /**
         * 镜头类型
         */
        private String shotType;
        
        /**
         * 镜头类型描述
         */
        private String shotTypeDesc;
        
        /**
         * 场景描述（中文）
         */
        private String descriptionCn;
        
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
        
        /**
         * 原始文本
         */
        private String originalText;
    }
}

