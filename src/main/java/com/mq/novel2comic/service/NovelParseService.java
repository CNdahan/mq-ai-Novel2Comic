package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.novel.NovelStructure;

/**
 * 小说文本解析服务接口
 * 负责对小说文本进行预处理、分段、提取关键信息
 */
public interface NovelParseService {

    /**
     * 解析小说文本结构
     * @param text 原始小说文本
     * @return 结构化的小说数据
     */
    NovelStructure parse(String text);

    /**
     * 清洗文本
     * @param text 原始文本
     * @return 清洗后的文本
     */
    String cleanText(String text);

    /**
     * 段落分割
     * @param text 文本内容
     * @return 段落列表
     */
    java.util.List<String> splitParagraphs(String text);

    /**
     * 识别对话
     * @param text 文本内容
     * @return 对话列表
     */
    java.util.List<String> extractDialogues(String text);
}
