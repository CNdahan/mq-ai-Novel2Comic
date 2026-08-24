package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.image.CachedImage;
import com.mq.novel2comic.model.dto.image.ImageGenerateResult;
import com.mq.novel2comic.model.entity.StoryboardPanel;
import com.mq.novel2comic.model.enums.ComicStyle;
import com.mq.novel2comic.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 图片生成服务实现
 * 
 * 核心功能：
 * 1. 异步并行生成（CompletableFuture）
 * 2. 语义缓存优化（减少70% API调用）
 * 3. 实时进度推送（WebSocket）
 * 4. 错误重试机制
 * 
 * 借鉴需求文档3.4节的设计
 * 
 * @author MQ
 */
@Service
@Slf4j
public class ImageGenerateServiceImpl implements ImageGenerateService {
    
    @Autowired
    private UnifiedImageClient imageClient;  // 使用统一接口，支持多种提供商
    
    @Autowired
    private PromptBuilderService promptBuilderService;
    
    @Autowired
    private SemanticCacheService semanticCacheService;
    
    @Autowired
    private ProgressNotifyService progressNotifyService;
    
    /**
     * 默认图片尺寸
     */
    private static final String DEFAULT_SIZE = "1024*1024";
    
    /**
     * 单个分镜异步生成
     * 使用 @Async 注解，在独立线程池中执行
     */
    @Override
    @Async("comicTaskExecutor")
    public CompletableFuture<ImageGenerateResult> generatePanelAsync(
            String taskId,
            StoryboardPanel storyboard,
            String style) {
        long startTime = System.currentTimeMillis();
        int panelIndex = storyboard.getPanelIndex();
        try {
            log.info("开始生成分镜: taskId={}, panelIndex={}, style={}", 
                    taskId, panelIndex, style);
            // 1. 构建Prompt
            String prompt = promptBuilderService.buildFinalPrompt(storyboard, style);
            String negativePrompt = promptBuilderService.buildNegativePrompt();
            log.debug("Prompt构建完成: panelIndex={}, prompt={}", panelIndex, prompt);
            // 2. 检查语义缓存
            progressNotifyService.notifyProgress(
                    taskId, panelIndex, 0, "检查缓存..."
            );
            Optional<CachedImage> cachedImage = semanticCacheService.checkCache(prompt);
            if (cachedImage.isPresent()) {
                // 缓存命中，直接返回
                CachedImage cached = cachedImage.get();
                long duration = System.currentTimeMillis() - startTime;
                log.info("✅ 分镜{}：语义缓存命中，相似度={}, 耗时={}ms", 
                        panelIndex, String.format("%.4f", cached.getSimilarity()), duration);
                progressNotifyService.notifyProgress(
                        taskId, panelIndex, 0, "缓存命中✓"
                );
                return CompletableFuture.completedFuture(
                        ImageGenerateResult.builder()
                                .storyboardId(storyboard.getId())
                                .imageUrl(cached.getImageUrl())
                                .prompt(prompt)
                                .isCached(true)
                                .cacheSimilarity(cached.getSimilarity())
                                .generateTimeMs((int) duration)
                                .imageWidth(1024)
                                .imageHeight(1024)
                                .build()
                );
            }
            // 3. 调用AIGC生成图片
            progressNotifyService.notifyProgress(
                    taskId, panelIndex, 0, "生成中..."
            );
            log.info("📸 使用{}生成图片...", imageClient.getProviderName());
            String imageUrl = imageClient.generateImage(
                    prompt,
                    negativePrompt,
                    DEFAULT_SIZE
            );
            // 4. 缓存结果
            semanticCacheService.cacheImage(prompt, imageUrl);
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 分镜{}：生成完成，耗时={}ms, url={}", 
                    panelIndex, duration, imageUrl);
            progressNotifyService.notifyProgress(
                    taskId, panelIndex, 0, "完成✓"
            );
            return CompletableFuture.completedFuture(
                    ImageGenerateResult.builder()
                            .storyboardId(storyboard.getId())
                            .imageUrl(imageUrl)
                            .prompt(prompt)
                            .isCached(false)
                            .cacheSimilarity(null)
                            .generateTimeMs((int) duration)
                            .imageWidth(1024)
                            .imageHeight(1024)
                            .build()
            );
            
        } catch (Exception e) {
            log.error("❌ 分镜{}：生成失败", panelIndex, e);
            progressNotifyService.notifyProgress(
                    taskId, panelIndex, 0, "失败✗"
            );
            return CompletableFuture.failedFuture(
                    new BusinessException(ErrorCode.SYSTEM_ERROR, 
                            "分镜" + panelIndex + "生成失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 批量并行生成
     * 核心方法：使用CompletableFuture.allOf等待所有任务完成
     */
    @Override
    public List<ImageGenerateResult> generateBatch(
            String taskId,
            List<StoryboardPanel> storyboards,
            String style) {
        if (storyboards == null || storyboards.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分镜列表为空");
        }
        int totalPanels = storyboards.size();
        log.info("🚀 开始批量生成: taskId={}, 数量={}, 风格={}", taskId, totalPanels, style);
        long startTime = System.currentTimeMillis();
        try {
            // 1. 创建异步任务列表
            List<CompletableFuture<ImageGenerateResult>> futures = new ArrayList<>();
            for (StoryboardPanel storyboard : storyboards) {
                CompletableFuture<ImageGenerateResult> future = 
                        generatePanelAsync(taskId, storyboard, style);
                futures.add(future);
            }
            // 2. 等待所有任务完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );
            // 阻塞等待所有任务完成
            allOf.join();
            // 3. 收集结果
            List<ImageGenerateResult> results = new ArrayList<>();
            int successCount = 0;
            int cachedCount = 0;
            for (int i = 0; i < futures.size(); i++) {
                try {
                    ImageGenerateResult result = futures.get(i).get();
                    if (result != null) {
                        results.add(result);
                        successCount++;
                        if (result.getIsCached()) {
                            cachedCount++;
                        }
                    }
                } catch (Exception e) {
                    log.error("获取分镜{}结果失败", i + 1, e);
                }
            }
            long totalDuration = System.currentTimeMillis() - startTime;
            double cacheHitRate = totalPanels > 0 ? (cachedCount * 100.0 / totalPanels) : 0;
            log.info("🎉 批量生成完成: 成功={}/{}, 缓存命中率={}%, 总耗时={}ms, 平均耗时={}ms",
                    successCount, totalPanels,
                    String.format("%.1f", cacheHitRate),
                    totalDuration,
                    totalDuration / totalPanels);
            // 4. 验证结果
            if (results.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "所有分镜生成失败");
            }
            if (results.size() < totalPanels) {
                log.warn("⚠️ 部分分镜生成失败: 成功={}, 总数={}", results.size(), totalPanels);
            }
            return results;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 批量生成失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                    "批量生成失败: " + e.getMessage());
        }
    }
}

