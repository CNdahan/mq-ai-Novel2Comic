package com.mq.novel2comic.model.dto.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 通义万相API响应对象
 * @author MQ
 */
@Data
public class WanxResponse {
    
    /**
     * 请求ID
     */
    @JsonProperty("request_id")
    private String requestId;
    
    /**
     * 输出结果
     */
    private Output output;
    
    /**
     * 使用量信息
     */
    private Usage usage;
    
    /**
     * 错误码
     */
    private String code;
    
    /**
     * 错误信息
     */
    private String message;
    
    @Data
    public static class Output {
        /**
         * 任务ID（异步任务）
         */
        @JsonProperty("task_id")
        private String taskId;
        
        /**
         * 任务状态
         */
        @JsonProperty("task_status")
        private String taskStatus;
        
        /**
         * 生成结果列表
         */
        private List<Result> results;
        
        /**
         * 任务提交时间
         */
        @JsonProperty("submit_time")
        private String submitTime;
        
        /**
         * 任务完成时间
         */
        @JsonProperty("scheduled_time")
        private String scheduledTime;
        
        /**
         * 任务结束时间
         */
        @JsonProperty("end_time")
        private String endTime;
    }
    
    @Data
    public static class Result {
        /**
         * 图片URL
         */
        private String url;
        
        /**
         * 图片代码（可能返回）
         */
        private String code;
        
        /**
         * 错误信息（可能返回）
         */
        private String message;
    }
    
    @Data
    public static class Usage {
        /**
         * 图片数量
         */
        @JsonProperty("image_count")
        private Integer imageCount;
    }
}

