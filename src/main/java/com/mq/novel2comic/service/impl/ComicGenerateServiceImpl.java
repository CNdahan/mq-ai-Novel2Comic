package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.IdUtil;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.comic.ComicGenerateRequest;
import com.mq.novel2comic.model.dto.comic.ComicGenerateResponse;
import com.mq.novel2comic.model.dto.image.ImageGenerateResult;
import com.mq.novel2comic.model.dto.novel.NovelStructure;
import com.mq.novel2comic.model.entity.*;
import com.mq.novel2comic.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 漫画生成核心业务编排服务
 * 串联所有模块完成完整的小说→漫画流程
 * 
 * @author MQ
 */
@Service
@Slf4j
public class ComicGenerateServiceImpl {
    
    @Resource
    private NovelService novelService;
    
    @Resource
    private StoryboardPanelService storyboardPanelService;
    
    @Resource
    private StoryboardService storyboardService;
    
    @Resource
    private StoryboardVersionService storyboardVersionService;
    
    @Resource
    private ImageGenerateService imageGenerateService;
    
    @Resource
    private ComicService comicService;
    
    @Resource
    private ComicPanelService comicPanelService;
    
    @Resource
    private GenerateTaskService generateTaskService;
    
    @Resource
    private ProgressNotifyService progressNotifyService;
    
    @Resource
    private ImageStorageService imageStorageService;
    
    /**
     * 核心方法：完整的漫画生成流程
     */
    @Transactional(rollbackFor = Exception.class)
    public ComicGenerateResponse generateComic(ComicGenerateRequest request, Long userId) {
        Long novelId = request.getNovelId();
        String style = request.getStyle();
        Boolean regenerateStoryboard = request.getRegenerateStoryboard();
        Integer storyboardVersion = request.getStoryboardVersion();
        log.info("开始生成漫画：novelId={}, userId={}, style={}, regenerate={}, version={}", 
                novelId, userId, style, regenerateStoryboard, storyboardVersion);
        // 1. 验证小说存在
        Novel novel = novelService.getById(novelId);
        if (novel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "小说不存在");
        }
        // 验证小说所有权
        if (!novel.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作此小说");
        }
        // 2. 创建任务
        String taskId = IdUtil.simpleUUID();
        GenerateTask task = createTask(taskId, novelId, userId);
        try {
            // 3. 获取或生成分镜（支持指定版本）
            List<StoryboardPanel> storyboards = getOrCreateStoryboards(
                    novelId, novel.getNovelContent(), regenerateStoryboard, storyboardVersion
            );
            if (storyboards.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分镜生成失败，请重试");
            }
            log.info("分镜准备完成：novelId={}, 分镜数={}", novelId, storyboards.size());
            // 更新任务信息
            task.setTotalPanels(storyboards.size());
            task.setCompletedPanels(0);
            generateTaskService.updateById(task);
            // 4. 批量生成图片（异步并行）
            generateTaskService.updateProgress(taskId, 10, "开始生成图片");
            List<ImageGenerateResult> images = imageGenerateService.generateBatch(
                    taskId, storyboards, style
            );
            log.info("图片生成完成：novelId={}, 成功={}/{}", 
                    novelId, images.size(), storyboards.size());
            // 5. 保存漫画
            generateTaskService.updateProgress(taskId, 90, "保存漫画数据");
            Comic comic = saveComic(novel, images, style, task);
            // 6. 更新任务状态
            generateTaskService.completeTask(taskId);
            // 7. 推送完成通知（包含comicId）
            progressNotifyService.notifyCompleted(taskId, comic.getId());
            log.info("漫画生成完成：novelId={}, comicId={}", novelId, comic.getId());
            // 8. 计算预估时间
            String estimatedTime = calculateEstimatedTime(images.size());
            return ComicGenerateResponse.builder()
                    .comicId(comic.getId())
                    .taskId(taskId)
                    .status("completed")
                    .panelCount(images.size())
                    .estimatedTime(estimatedTime)
                    .build();
        } catch (Exception e) {
            log.error("漫画生成失败：novelId={}", novelId, e);
            generateTaskService.failTask(taskId, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                    "漫画生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建生成任务
     */
    private GenerateTask createTask(String taskId, Long novelId, Long userId) {
        GenerateTask task = new GenerateTask();
        task.setTaskUuid(taskId);
        task.setNovelId(novelId);
        task.setUserId(userId);
        task.setTaskType("comic_generation");
        task.setStatus("processing");
        task.setProgressPercent(0);
        task.setCurrentStep("初始化");
        task.setStartTime(new Date());
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        task.setIsDelete(0);
        generateTaskService.save(task);
        return task;
    }
    
    /**
     * 获取或创建分镜（支持指定版本）
     */
    private List<StoryboardPanel> getOrCreateStoryboards(
            Long novelId, String novelContent, Boolean regenerate, Integer version) {
        // 如果不需要重新生成，先查询已有分镜
        if (!Boolean.TRUE.equals(regenerate)) {
            // 使用版本服务获取指定版本或当前版本的分镜
            List<StoryboardPanel> existing = storyboardVersionService.getVersionPanels(novelId, version);
            if (!existing.isEmpty()) {
                Integer actualVersion = existing.get(0).getVersion();
                log.info("使用已有分镜：novelId={}, version={}, 数量={}", 
                        novelId, actualVersion, existing.size());
                return existing;
            }
            // 如果指定了版本但不存在，抛出异常
            if (version != null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, 
                        "指定的分镜版本不存在: " + version);
            }
        }
        // 生成新分镜（这部分逻辑应该通过StoryboardController来完成，这里只是兼容）
        log.warn("漫画生成过程中触发新分镜生成，建议先调用分镜生成接口：novelId={}", novelId);
        // 解析小说
        NovelStructure structure = novelService.parseNovel(novelContent);
        // 生成分镜脚本
        List<com.mq.novel2comic.model.dto.novel.StoryboardPanel> panels = 
                storyboardService.generateStoryboard(structure);
        // 确定新版本号
        Integer newVersion = storyboardVersionService.getNextVersion(novelId);
        // 保存到数据库
        List<StoryboardPanel> savedPanels = new ArrayList<>();
        for (int i = 0; i < panels.size(); i++) {
            com.mq.novel2comic.model.dto.novel.StoryboardPanel panel = panels.get(i);
            StoryboardPanel entity = new StoryboardPanel();
            entity.setNovelId(novelId);
            entity.setVersion(newVersion);
            entity.setIsCurrent(1);
            entity.setPanelIndex(i + 1);
            entity.setSceneType(panel.getSceneType());
            entity.setShotType(panel.getShotType());
            entity.setDescriptionCn(panel.getDescriptionCn());
            entity.setDescriptionEn(panel.getPrompt());
            // 直接设置List对象，JacksonTypeHandler会自动处理JSON序列化
            entity.setCharacterList(panel.getCharacters());
            entity.setEnvironment(panel.getEnvironment());
            entity.setMood(panel.getMood());
            entity.setDialogueText(panel.getOriginalText());
            entity.setIsDelete(0);
            storyboardPanelService.save(entity);
            savedPanels.add(entity);
        }
        // 设置为当前版本
        storyboardVersionService.setCurrentVersion(novelId, newVersion);
        log.info("新分镜保存完成：novelId={}, version={}, 数量={}", 
                novelId, newVersion, savedPanels.size());
        return savedPanels;
    }
    
    /**
     * 保存漫画
     */
    private Comic saveComic(Novel novel, List<ImageGenerateResult> images, 
                            String style, GenerateTask task) {
        // 1. 创建漫画主记录
        Comic comic = new Comic();
        comic.setNovelId(novel.getId());
        comic.setUserId(novel.getUserId());
        comic.setComicTitle(novel.getNovelTitle() + " - 漫画版");
        comic.setStyle(style);
        comic.setPanelCount(images.size());
        comic.setStatus("completed");
        comic.setIsDelete(0);
        comicService.save(comic);
        log.info("漫画主记录保存成功：comicId={}", comic.getId());
        // 2. 保存所有面板（下载并持久化图片）
        int successCount = 0;
        int failedCount = 0;
        for (int i = 0; i < images.size(); i++) {
            ImageGenerateResult image = images.get(i);
            // 🔑 关键：下载并保存图片到本地（解决URL过期问题）
            String ossUrl = image.getImageUrl();
            String localUrl = imageStorageService.downloadAndSave(
                ossUrl, 
                comic.getId(), 
                i + 1
            );
            // 处理下载失败的情况
            String finalUrl;
            if (localUrl != null) {
                finalUrl = localUrl;
                successCount++;
                log.info("✅ 面板{}图片持久化成功：OSS URL → 本地URL", i + 1);
            } else {
                // 下载失败，暂时使用OSS URL（会在1小时后过期）
                finalUrl = ossUrl;
                failedCount++;
                log.warn("⚠️ 面板{}图片下载失败，暂时使用OSS URL（1小时后会过期）", i + 1);
            }
            ComicPanel panel = new ComicPanel();
            panel.setComicId(comic.getId());
            panel.setNovelId(novel.getId());
            panel.setStoryboardId(image.getStoryboardId());
            panel.setPanelIndex(i + 1);
            panel.setImageUrl(finalUrl);
            panel.setPromptText(image.getPrompt());
            panel.setGenerateTimeMs(image.getGenerateTimeMs());
            panel.setIsCached(image.getIsCached() ? 1 : 0);
            panel.setStyle(style);
            panel.setIsDelete(0);
            comicPanelService.save(panel);
        }
        log.info("漫画面板保存完成：comicId={}, 面板数={}, 持久化成功={}, 失败={}", 
                comic.getId(), images.size(), successCount, failedCount);
        return comic;
    }
    
    /**
     * 计算预估时间
     */
    private String calculateEstimatedTime(int panelCount) {
        // 假设每个分镜平均生成时间为15-20秒
        int seconds = panelCount * 18;
        int minutes = seconds / 60;
        if (minutes == 0) {
            return "< 1分钟";
        } else if (minutes <= 2) {
            return "2-3分钟";
        } else {
            return minutes + "-" + (minutes + 1) + "分钟";
        }
    }
}

