package com.mq.novel2comic.model.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM请求DTO
 * 兼容OpenAI格式（智谱AI和DeepSeek都支持）
 * 
 * @author MQ
 * @date 2025-10-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMRequest {
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 消息列表
     */
    private List<Message> messages;
    
    /**
     * 温度参数（0-1）
     */
    private Double temperature;
    
    /**
     * 最大token数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色：system, user, assistant
         */
        private String role;
        
        /**
         * 消息内容
         */
        private String content;
    }
}


