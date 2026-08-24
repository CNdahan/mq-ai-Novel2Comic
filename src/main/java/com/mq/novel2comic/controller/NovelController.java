package com.mq.novel2comic.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.model.dto.novel.*;
import com.mq.novel2comic.service.NovelRecommendService;
import com.mq.novel2comic.service.NovelService;
import com.mq.novel2comic.utils.JwtUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 获取每日推荐小说
     */
    @GetMapping("/recommendations")
    public BaseResponse<List<NovelRecommendDTO>> getDailyRecommendations() {
        List<NovelRecommendDTO> recommendations = novelRecommendService.getDailyRecommendations();
        return ResultUtils.success(recommendations, "获取推荐小说成功");
    }
}
