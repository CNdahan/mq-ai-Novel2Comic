package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.service.ProgressNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

/**
 * 进度通知服务实现
 * 使用WebSocket推送实时进度
 * 
 * @author MQ
 */
@Service
@Slf4j
public class ProgressNotifyServiceImpl implements ProgressNotifyService {
    
    @Autowired(required = false) // 可选依赖，如果没有配置WebSocket也能正常运行
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * WebSocket主题前缀
     */
    private static final String TOPIC_PREFIX = "/topic/progress/";
    
    /**
     * 推送任务进度
     */
    @Override
    public void notifyProgress(String taskId, int currentPanel, int totalPanels, String status) {
        if (messagingTemplate == null) {
            log.debug("WebSocket未配置，跳过进度推送");
            return;
        }
        
        try {
            int percentage = totalPanels > 0 ? (currentPanel * 100 / totalPanels) : 0;
            
            Map<String, Object> message = new HashMap<>();
            message.put("type", "progress");
            message.put("taskId", taskId);
            message.put("status", "processing");
            message.put("progress", percentage);
            message.put("currentStep", status);
            message.put("completedPanels", currentPanel);
            message.put("totalPanels", totalPanels);
            message.put("timestamp", System.currentTimeMillis());
            
            String destination = TOPIC_PREFIX + taskId;
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("推送进度: taskId={}, {}/{}, {}%", 
                    taskId, currentPanel, totalPanels, percentage);
            
        } catch (Exception e) {
            log.error("推送进度失败: taskId={}", taskId, e);
        }
    }
    
    /**
     * 推送任务完成
     */
    @Override
    public void notifyCompleted(String taskId, Long comicId) {
        if (messagingTemplate == null) {
            return;
        }
        
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "completed");
            message.put("taskId", taskId);
            message.put("comicId", comicId);
            message.put("status", "completed");
            message.put("timestamp", System.currentTimeMillis());
            
            String destination = TOPIC_PREFIX + taskId;
            messagingTemplate.convertAndSend(destination, message);
            
            log.info("✅ 任务完成通知: taskId={}, comicId={}", taskId, comicId);
            
        } catch (Exception e) {
            log.error("推送完成通知失败: taskId={}", taskId, e);
        }
    }
    
    /**
     * 推送任务失败
     */
    @Override
    public void notifyFailed(String taskId, String errorMessage) {
        if (messagingTemplate == null) {
            return;
        }
        
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "failed");
            message.put("taskId", taskId);
            message.put("status", "failed");
            message.put("errorMessage", errorMessage);
            message.put("timestamp", System.currentTimeMillis());
            
            String destination = TOPIC_PREFIX + taskId;
            messagingTemplate.convertAndSend(destination, message);
            
            log.error("❌ 任务失败通知: taskId={}, error={}", taskId, errorMessage);
            
        } catch (Exception e) {
            log.error("推送失败通知失败: taskId={}", taskId, e);
        }
    }
}

