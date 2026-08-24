package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.service.UnifiedImageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Mock图片生成适配器
 * 用于开发调试，快速验证功能
 * 
 * 激活方式：
 * aigc.provider=mock
 * 
 * @author MQ
 */
@Service
@ConditionalOnProperty(name = "aigc.provider", havingValue = "mock")
@Primary
@Slf4j
public class MockImageAdapter implements UnifiedImageClient {
    
    /**
     * Mock图片URL列表
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
    
    @Override
    public String generateImage(String prompt, String negativePrompt, String size) {
        log.info("🎨 [MOCK] 生成图片, prompt前50字={}", 
                prompt.substring(0, Math.min(50, prompt.length())));
        try {
            // 模拟1-2秒延迟
            int delay = 1000 + (int) (Math.random() * 1000);
            Thread.sleep(delay);
            // 根据prompt hash选择图片（相同prompt返回相同图片）
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
    public String getProviderName() {
        return "Mock模式（开发调试）";
    }
}

