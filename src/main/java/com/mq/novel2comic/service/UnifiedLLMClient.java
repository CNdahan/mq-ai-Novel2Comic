package com.mq.novel2comic.service;

/**
 * 统一LLM客户端接口
 * 支持多种LLM提供商：智谱AI、DeepSeek、通义千问等
 * 
 * 设计思想：
 * - 适配器模式：统一接口，屏蔽底层差异
 * - 配置驱动：通过llm.provider一键切换
 * - 易扩展：新增提供商只需实现此接口
 * 
 * @author MQ
 * @date 2025-10-26
 */
public interface UnifiedLLMClient {
    
    /**
     * 发送提示词，获取响应
     * 
     * @param prompt 用户提示词
     * @param systemPrompt 系统提示词（角色设定）
     * @return LLM响应内容
     */
    String chat(String prompt, String systemPrompt);
    
    /**
     * 获取提供商名称
     * 用于日志记录和监控
     * 
     * @return 提供商名称，如"智谱AI (GLM-4-Flash)"
     */
    String getProviderName();
    
    /**
     * 检查服务是否可用
     * 
     * @return true表示服务正常，false表示服务不可用
     */
    default boolean isAvailable() {
        return true;
    }
}


