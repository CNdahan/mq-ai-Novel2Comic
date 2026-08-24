package com.mq.novel2comic.model.dto.image;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通义万相API请求对象
 * @author MQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // 排除null值
public class WanxRequest {
    
    /**
     * 模型名称，固定为 wanx-v1
     */
    private String model;
    
    /**
     * 输入参数
     */
    private Input input;
    
    /**
     * 生成参数
     */
    private Parameters parameters;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)  // 排除null值
    public static class Input {
        /**
         * 正向提示词
         */
        private String prompt;
        
        /**
         * 负向提示词
         */
        @JsonProperty("negative_prompt")
        private String negativePrompt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)  // 排除null值
    public static class Parameters {
        /**
         * 风格，如 <anime>、<3d cartoon>、<flat illustration>
         */
        private String style;
        
        /**
         * 图片尺寸，如 1024*1024、720*1280、1280*720
         */
        private String size;
        
        /**
         * 生成图片数量，默认1
         */
        private Integer n;
        
        /**
         * 随机种子，用于复现，范围 [0, 4294967290]
         * 不设置时由系统随机生成
         */
        private Integer seed;
    }
}

