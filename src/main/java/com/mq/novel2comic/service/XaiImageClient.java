package com.mq.novel2comic.service;

/**
 * xAI Grok 图片生成客户端
 */
public interface XaiImageClient {

    String generateImage(String prompt, String negativePrompt, String size);
}
