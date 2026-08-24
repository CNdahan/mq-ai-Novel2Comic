package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.model.dto.image.WanxRequest;
import com.mq.novel2comic.model.dto.image.WanxResponse;
import com.mq.novel2comic.service.WanxImageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Mock版本的通义万相客户端
 * 用于开发测试，避免每次都调用真实API
 * 
 * 激活方式：
 * 1. 在application.yml中设置 spring.profiles.active=dev
 * 2. 或运行时添加参数：-Dspring.profiles.active=dev
 * 
 * @author MQ
 */
@Service
@Profile("dev")  // 仅在dev环境激活
@Primary  // 优先使用此实现
@Slf4j
public class MockWanxImageClient implements WanxImageClient {
    
    /**
     * Mock图片URL列表（使用公共占位图服务）
     */
    private static final String[] MOCK_URLS = {
        "https://picsum.photos/1024/1024?random=1",
        "https://picsum.photos/1024/1024?random=2",
        "https://picsum.photos/1024/1024?random=3",
        "https://picsum.photos/1024/1024?random=4",
        "https://picsum.photos/1024/1024?random=5",
        "https://picsum.photos/1024/1024?random=6",
        "https://picsum.photos/1024/1024?random=7",
        "https://picsum.photos/1024/1024?random=8",
        "https://picsum.photos/1024/1024?random=9",
        "https://picsum.photos/1024/1024?random=10"
    };
    
    /**
     * Mock生成图片
     * 模拟1-2秒的生成延迟
     */
    @Override
    public String generateImage(String prompt, String negativePrompt, String style, String size) {
        log.info("🎨 [MOCK] 生成图片, prompt={}", prompt.substring(0, Math.min(50, prompt.length())));
        try {
            // 模拟API延迟（1-2秒）
            int delay = 1000 + (int) (Math.random() * 1000);
            Thread.sleep(delay);
            // 根据prompt的hash选择不同的图片（保证相同prompt返回相同图片）
            int index = Math.abs(prompt.hashCode()) % MOCK_URLS.length;
            String mockUrl = MOCK_URLS[index];
            log.info("✅ [MOCK] 生成完成, url={}, 耗时={}ms", mockUrl, delay);
            return mockUrl;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Mock生成被中断", e);
            return MOCK_URLS[0];
        }
    }
    
    @Override
    public String submitTask(WanxRequest request) {
        // Mock实现不使用异步任务
        log.info("🎨 [MOCK] 提交任务");
        return "mock-task-id-" + System.currentTimeMillis();
    }

    @Override
    public WanxResponse queryTask(String taskId) {
        // Mock实现不使用异步任务
        log.info("🎨 [MOCK] 查询任务: taskId={}", taskId);
        WanxResponse response = new WanxResponse();
        WanxResponse.Output output = new WanxResponse.Output();
        output.setTaskStatus("SUCCEEDED");
        response.setOutput(output);
        return response;
    }
}

