package com.mq.novel2comic.model.dto.image;

import lombok.Data;

import java.util.List;

/**
 * 硅基流动API响应对象
 * 
 * @author MQ
 */
@Data
public class SiliconFlowResponse {
    
    /**
     * 生成的图片列表
     */
    private List<ImageData> images;
    
    /**
     * 时间消耗信息
     */
    private TimingsData timings;
    
    /**
     * 随机种子
     */
    private Integer seed;
    
    @Data
    public static class ImageData {
        /**
         * 图片URL
         */
        private String url;
        
        /**
         * 图片Base64（可选）
         */
        private String b64Json;
    }
    
    @Data
    public static class TimingsData {
        /**
         * 推理时间（秒）
         */
        private Double inference;
    }
}

