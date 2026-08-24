package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.image.WanxRequest;
import com.mq.novel2comic.model.dto.image.WanxResponse;

/**
 * 通义万相图片生成客户端
 * @author MQ
 */
public interface WanxImageClient {
    
    /**
     * 生成图片（同步接口）
     * @param prompt 正向提示词
     * @param negativePrompt 负向提示词
     * @param style 风格
     * @param size 尺寸
     * @return 图片URL
     */
    String generateImage(String prompt, String negativePrompt, String style, String size);
    
    /**
     * 提交异步生成任务
     * @param request 请求对象
     * @return 任务ID
     */
    String submitTask(WanxRequest request);
    
    /**
     * 查询任务状态
     * @param taskId 任务ID
     * @return 响应对象
     */
    WanxResponse queryTask(String taskId);
}

