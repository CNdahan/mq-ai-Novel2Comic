package com.mq.novel2comic.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 重试机制配置
 * @author MQ
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry 自动配置
    // 在需要重试的方法上使用 @Retryable 注解
}

