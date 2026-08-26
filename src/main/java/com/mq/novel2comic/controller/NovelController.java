package com.mq.novel2comic.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.comic.ComicListItemResponse;
import com.mq.novel2comic.model.entity.Comic;
import com.mq.novel2comic.model.entity.ComicPanel;
import com.mq.novel2comic.model.entity.GenerateTask;
import com.mq.novel2comic.model.entity.Novel;
import com.mq.novel2comic.model.entity.StoryboardPanel;
import com.mq.novel2comic.model.dto.novel.*;
import com.mq.novel2comic.service.ComicPanelService;
import com.mq.novel2comic.service.ComicService;
import com.mq.novel2comic.service.CharacterProfileService;
import com.mq.novel2comic.service.GenerateTaskService;
import com.mq.novel2comic.service.ImageStorageService;
import com.mq.novel2comic.service.NovelRecommendService;
import com.mq.novel2comic.service.NovelService;
import com.mq.novel2comic.service.StoryboardPanelService;
import com.mq.novel2comic.utils.JwtUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.ZoneId;

/**
 * 小说管理控制器
 */
@RestController
@RequestMapping("/novel")
public class NovelController {

    @Resource
    private NovelService novelService;

    @Resource
    private NovelRecommendService novelRecommendService;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private ComicService comicService;

    @Resource
    private ComicPanelService comicPanelService;

    @Resource
    private StoryboardPanelService storyboardPanelService;

    @Resource
    private GenerateTaskService generateTaskService;

    @Resource
    private CharacterProfileService characterProfileService;

    @Resource
    private ImageStorageService imageStorageService;

    /**
     * 上传小说文本
     */
    @PostMapping("/upload")
    public BaseResponse<NovelUploadResponse> uploadNovel(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody NovelUploadRequest request) {
        // 从Token获取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        NovelUploadResponse response = novelService.uploadNovel(request, userId);
        return ResultUtils.success(response, "小说上传成功");
    }

    /**
     * 获取小说详情
     */
    @GetMapping("/{novelId}")
    public BaseResponse<NovelDetailResponse> getNovelDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long novelId) {
        // 从Token获取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        NovelDetailResponse response = novelService.getNovelDetail(novelId, userId);
        return ResultUtils.success(response);
    }

    /**
     * 获取用户小说列表
     */
    @GetMapping("/list")
    public BaseResponse<IPage<NovelListItem>> getUserNovelList(
            @RequestHeader("Authorization") String authHeader,
            NovelQueryRequest request) {
        // 从Token获取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        IPage<NovelListItem> response = novelService.getUserNovelList(request, userId);
        return ResultUtils.success(response);
    }

    /**
     * 重新提取角色。用于首次上传时 LLM 临时失败的小说，无需重新上传原文。
     */
    @PostMapping("/{novelId}/retry-character-extraction")
    public BaseResponse<Boolean> retryCharacterExtraction(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long novelId) {
        Long userId = getUserId(authHeader);
        boolean result = novelService.retryCharacterExtraction(novelId, userId);
        return ResultUtils.success(result, "角色重新提取成功");
    }

    /**
     * 获取按小说聚合的创作工作流列表，包含尚未生成漫画的小说。
     */
    @GetMapping("/workflow-list")
    public BaseResponse<List<ComicListItemResponse>> getWorkflowList(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserId(authHeader);
        List<Novel> novels = novelService.lambdaQuery()
                .eq(Novel::getUserId, userId)
                .eq(Novel::getIsDelete, 0)
                .orderByDesc(Novel::getCreateTime)
                .list();

        List<ComicListItemResponse> items = novels.stream().map(this::toWorkflowItem).toList();
        return ResultUtils.success(items);
    }

    /** 删除尚未生成漫画的小说。 */
    @DeleteMapping("/{novelId}")
    public BaseResponse<Boolean> deleteNovel(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long novelId) {
        Long userId = getUserId(authHeader);
        Novel novel = novelService.getById(novelId);
        if (novel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "小说不存在");
        }
        if (!userId.equals(novel.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该小说");
        }
        List<Comic> comics = comicService.lambdaQuery()
                .eq(Comic::getNovelId, novelId)
                .eq(Comic::getUserId, userId)
                .eq(Comic::getIsDelete, 0)
                .list();
        for (Comic comic : comics) {
            imageStorageService.deleteAllImages(comic.getId(), comic.getPanelCount() == null ? 0 : comic.getPanelCount());
            comicService.update(new UpdateWrapper<Comic>()
                    .eq("id", comic.getId())
                    .eq("userId", userId)
                    .eq("isDelete", 0)
                    .set("isDelete", 1));
        }
        return ResultUtils.success(novelService.removeById(novelId), "作品删除成功");
    }

    /**
     * 获取每日推荐小说
     */
    @GetMapping("/recommendations")
    public BaseResponse<List<NovelRecommendDTO>> getDailyRecommendations() {
        List<NovelRecommendDTO> recommendations = novelRecommendService.getDailyRecommendations();
        return ResultUtils.success(recommendations, "获取推荐小说成功");
    }

    private ComicListItemResponse toWorkflowItem(Novel novel) {
        Comic comic = comicService.lambdaQuery()
                .eq(Comic::getNovelId, novel.getId())
                .eq(Comic::getUserId, novel.getUserId())
                .eq(Comic::getIsDelete, 0)
                .orderByDesc(Comic::getCreateTime)
                .last("LIMIT 1")
                .one();
        List<StoryboardPanel> storyboards = storyboardPanelService.lambdaQuery()
                .eq(StoryboardPanel::getNovelId, novel.getId())
                .eq(StoryboardPanel::getIsDelete, 0)
                .eq(StoryboardPanel::getIsCurrent, 1)
                .list();
        long characterCount = characterProfileService.lambdaQuery()
                .eq(com.mq.novel2comic.model.entity.CharacterProfile::getNovelId, novel.getId())
                .eq(com.mq.novel2comic.model.entity.CharacterProfile::getIsDelete, 0)
                .count();
        GenerateTask task = generateTaskService.lambdaQuery()
                .eq(GenerateTask::getNovelId, novel.getId())
                .eq(GenerateTask::getUserId, novel.getUserId())
                .eq(GenerateTask::getIsDelete, 0)
                .orderByDesc(GenerateTask::getCreateTime)
                .last("LIMIT 1")
                .one();

        String status;
        if (task != null && "processing".equals(task.getStatus())) {
            status = "storyboard_generation".equals(task.getTaskType())
                    ? "storyboard_generating" : "image_generating";
        } else if (task != null && "completed".equals(task.getStatus())
                && "storyboard_generation".equals(task.getTaskType())) {
            status = "storyboard_completed";
        } else if (comic != null && "completed".equals(comic.getStatus())) {
            status = "completed";
        } else if (storyboards.isEmpty()) {
            status = characterCount > 0 ? "storyboard_pending" : "uploaded";
        } else if (task != null && "failed".equals(task.getStatus())) {
            status = "storyboard_generation".equals(task.getTaskType())
                    ? "storyboard_pending" : "image_pending";
        } else {
            status = "storyboard_completed";
        }

        String cover = null;
        if (comic != null) {
            ComicPanel panel = comicPanelService.lambdaQuery()
                    .eq(ComicPanel::getComicId, comic.getId())
                    .eq(ComicPanel::getIsDelete, 0)
                    .orderByAsc(ComicPanel::getPanelIndex)
                    .last("LIMIT 1")
                    .one();
            cover = panel == null ? null : panel.getImageUrl();
        }
        java.util.Date updated = novel.getUpdateTime();
        if (comic != null && comic.getUpdateTime() != null &&
                (updated == null || comic.getUpdateTime().after(updated))) {
            updated = comic.getUpdateTime();
        }
        return ComicListItemResponse.builder()
                .comicId(comic == null ? null : comic.getId())
                .taskId(task == null ? null : task.getTaskUuid())
                .title(novel.getNovelTitle())
                .novelId(novel.getId())
                .novelTitle(novel.getNovelTitle())
                .style(comic == null ? null : comic.getStyle())
                .status(status)
                .panelCount(comic == null ? storyboards.size() : comic.getPanelCount())
                .coverImage(cover)
                .createdAt(toLocalDateTime(novel.getCreateTime()))
                .updatedAt(toLocalDateTime(updated))
                .build();
    }

    private Long getUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        Long userId = jwtUtils.getUserIdFromToken(authHeader.replace("Bearer ", ""));
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        return userId;
    }

    private java.time.LocalDateTime toLocalDateTime(java.util.Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
