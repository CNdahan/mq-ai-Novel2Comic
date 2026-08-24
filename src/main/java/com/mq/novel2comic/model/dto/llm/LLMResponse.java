package com.mq.novel2comic.model.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * LLM响应DTO
 * 兼容OpenAI格式
 * 
 * @author MQ
 * @date 2025-10-26
 */
@Data
public class LLMResponse {
    
    private String id;
    
    private String object;
    
    private Long created;
    
    private String model;
    
    private List<Choice> choices;
    
    private Usage usage;
    
    @Data
    public static class Choice {
        private Integer index;
        
        private Message message;
        
        @JsonProperty("finish_reason")
        private String finishReason;
    }
    
    @Data
    public static class Message {
        private String role;
        
        private String content;
    }
    
    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}


