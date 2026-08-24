package com.mq.novel2comic.service;

/**
 * OpenAI 图片生成客户端
 */
public interface OpenAIImageClient {

    String generateImage(String prompt, String negativePrompt, String size);
}
