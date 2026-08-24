package com.mq.novel2comic.model.dto.storyboard;

import lombok.Data;

import java.util.List;

/**
 * 分镜生成请求
 * 
 * @author MQ
 */
@Data
public class StoryboardGenerateRequest {
    
    /**
     * 小说ID
     */
    private Long novelId;
    
    /**
     * 期望的分镜数量（可选，系统会自动判断）
     */
    private Integer panelCount;
    
    /**
     * 自定义分镜（可选）
     */
    private List<CustomStoryboardPanel> customPanels;
    
    /**
     * 自定义分镜面板
     */
    @Data
    public static class CustomStoryboardPanel {
        /**
         * 分镜序号
         */
        private Integer index;
        
        /**
         * 场景描述
         */
        private String description;
        
        /**
         * 角色列表
         */
        private List<String> characters;
        
        /**
         * 镜头类型
         */
        private String shotType;
        
        /**
         * 环境描述
         */
        private String environment;
        
        /**
         * 情绪氛围
         */
        private String mood;
    }
}

