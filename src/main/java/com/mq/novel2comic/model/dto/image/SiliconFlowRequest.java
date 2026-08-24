package com.mq.novel2comic.model.dto.image;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 硅基流动API请求对象
 * API文档: https://docs.siliconflow.cn/api-reference/image-generation
 * 
 * @author MQ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiliconFlowRequest {
    
    /**
     * 模型名称
     * 推荐：
     * - stabilityai/stable-diffusion-xl-base-1.0 (速度快，质量好)
     * - black-forest-labs/FLUX.1-schnell (最新，质量最好)
     */
    private String model;
    
    /**
     * 正向提示词
     */
    private String prompt;
    
    /**
     * 负向提示词
     */
    @JsonProperty("negative_prompt")
    private String negativePrompt;
    
    /**
     * 图片尺寸
     * 格式: "宽x高"，如 "1024x1024"
     */
    @JsonProperty("image_size")
    private String imageSize;
    
    /**
     * 批次大小（生成图片数量）
     */
    @JsonProperty("batch_size")
    private Integer batchSize;
    
    /**
     * 随机种子
     */
    private Integer seed;
    
    /**
     * 引导系数（CFG Scale）
     */
    @JsonProperty("guidance_scale")
    private Double guidanceScale;
    
    /**
     * 采样步数
     */
    @JsonProperty("num_inference_steps")
    private Integer numInferenceSteps;
}

