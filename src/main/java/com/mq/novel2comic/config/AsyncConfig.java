package com.mq.novel2comic.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * @author MQ
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * 漫画生成任务线程池
     */
    @Bean(name = "comicTaskExecutor")
    public Executor comicTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据CPU核心数设置
        executor.setCorePoolSize(4);
        // 最大线程数：处理高峰期并发
        executor.setMaxPoolSize(10);
        // 队列容量：等待队列
        executor.setQueueCapacity(100);
        // 线程名称前缀
        executor.setThreadNamePrefix("comic-task-");
        // 拒绝策略：队列满时由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待超时时间
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("漫画生成任务线程池初始化完成");
        return executor;
    }
}
