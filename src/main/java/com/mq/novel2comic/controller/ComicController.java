package com.mq.novel2comic.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.comic.ComicDetailResponse;
import com.mq.novel2comic.model.dto.comic.ComicGenerateRequest;
import com.mq.novel2comic.model.dto.comic.ComicGenerateResponse;
import com.mq.novel2comic.model.dto.comic.ComicListItemResponse;
import com.mq.novel2comic.model.dto.comic.ComicQueryRequest;
import com.mq.novel2comic.model.entity.Comic;
import com.mq.novel2comic.model.entity.ComicPanel;
import com.mq.novel2comic.service.ComicPanelService;
import com.mq.novel2comic.service.ComicService;
import com.mq.novel2comic.service.ImageStorageService;
import com.mq.novel2comic.service.UserService;
import com.mq.novel2comic.service.impl.ComicGenerateServiceImpl;
import com.mq.novel2comic.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 漫画控制器
 * @author MQ
 */
@RestController
@RequestMapping("/comic")
@Slf4j
public class ComicController {

    @Resource
    private ComicService comicService;
    
    @Resource
    private ComicPanelService comicPanelService;
    
    @Resource
    private ComicGenerateServiceImpl comicGenerateService;
    
    @Resource
    private JwtUtils jwtUtils;
    
    @Resource
    private ImageStorageService imageStorageService;
    
    @Resource
    private UserService userService;

    /**
     * 生成漫画（核心接口）
     * POST /comic/generate
     */
    @PostMapping("/generate")
    public BaseResponse<ComicGenerateResponse> generateComic(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ComicGenerateRequest request) {
        log.info("收到漫画生成请求: {}", request);
        // 1. 从Token中提取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        // 2. 检查用户剩余次数
        if (!userService.hasQuota(userId)) {
            Integer remainQuota = userService.getQuotaRemain(userId);
            log.warn("⚠️ 用户次数不足: userId={}, quotaRemain={}", userId, remainQuota);
            throw new BusinessException(ErrorCode.QUOTA_INSUFFICIENT, 
                    "生成次数不足，当前剩余: " + remainQuota + " 次");
        }
        // 3. 扣减用户次数
        boolean deducted = userService.deductQuota(userId, 1);
        if (!deducted) {
            log.error("❌ 次数扣减失败: userId={}", userId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "次数扣减失败，请稍后重试");
        }
        // 4. 调用生成服务
        ComicGenerateResponse response = comicGenerateService.generateComic(request, userId);
        // 5. 在响应中包含剩余次数
        Integer remainQuota = userService.getQuotaRemain(userId);
        log.info("✅ 漫画生成完成，剩余次数: {}", remainQuota);
        return ResultUtils.success(response, "漫画生成成功，剩余次数: " + remainQuota);
    }
    
    /**
     * 获取漫画生成结果（包含所有面板）
     * GET /comic/result/{comicId}
     */
    @GetMapping("/result/{comicId}")
    public BaseResponse<ComicDetailResponse> getComicResult(
            @PathVariable Long comicId) {
        log.info("获取漫画结果: comicId={}", comicId);
        // 查询漫画主记录
        Comic comic = comicService.getById(comicId);
        if (comic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "漫画不存在");
        }
        // 查询所有面板
        QueryWrapper<ComicPanel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("comicId", comicId);  // 使用驼峰命名
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByAsc("panelIndex");
        List<ComicPanel> panels = comicPanelService.list(queryWrapper);
        // 构建响应
        ComicDetailResponse response = ComicDetailResponse.builder()
                .comicId(comic.getId())
                .title(comic.getComicTitle())
                .style(comic.getStyle())
                .status(comic.getStatus())
                .panelCount(comic.getPanelCount())
                .panels(panels)
                .build();
        return ResultUtils.success(response);
    }

    /**
     * 获取用户漫画列表（分页）
     * GET /comic/list
     */
    @GetMapping("/list")
    public BaseResponse<IPage<ComicListItemResponse>> getUserComicList(
            @RequestHeader("Authorization") String authHeader,
            ComicQueryRequest request) {
        log.info("获取用户漫画列表: request={}", request);
        // 从Token获取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        IPage<ComicListItemResponse> response = comicService.getUserComicList(request, userId);
        return ResultUtils.success(response);
    }
    
    /**
     * 根据novelId获取最新的漫画
     * GET /comic/latest/{novelId}
     */
    @GetMapping("/latest/{novelId}")
    public BaseResponse<Comic> getLatestComicByNovelId(@PathVariable Long novelId) {
        log.info("获取小说最新漫画: novelId={}", novelId);
        // 查询该小说最新的漫画（按创建时间降序）
        QueryWrapper<Comic> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("novelId", novelId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");
        queryWrapper.last("LIMIT 1");
        Comic latestComic = comicService.getOne(queryWrapper);
        if (latestComic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该小说还没有生成漫画");
        }
        return ResultUtils.success(latestComic);
    }
    
    /**
     * 根据ID获取漫画详情
     */
    @GetMapping("/{id}")
    public BaseResponse<Comic> getComicById(@PathVariable Long id) {
        Comic comic = comicService.getById(id);
        return ResultUtils.success(comic);
    }

    /**
     * 删除漫画（包括本地图片文件）
     * DELETE /comic/{id}
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteComic(@PathVariable Long id) {
        log.info("开始删除漫画: comicId={}", id);
        // 1. 查询漫画信息
        Comic comic = comicService.getById(id);
        if (comic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "漫画不存在");
        }
        // 2. 删除本地图片文件
        int panelCount = comic.getPanelCount();
        int deletedImageCount = imageStorageService.deleteAllImages(id, panelCount);
        log.info("本地图片清理完成: comicId={}, 删除了 {}/{} 个文件", 
                id, deletedImageCount, panelCount);
        // 3. 删除数据库记录（软删除）- 使用UpdateWrapper明确更新isDelete字段
        UpdateWrapper<Comic> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                     .eq("isDelete", 0)
                     .set("isDelete", 1);
        boolean result = comicService.update(updateWrapper);
        if (result) {
            log.info("✅ 漫画删除成功: comicId={}, title={}", id, comic.getComicTitle());
        } else {
            log.error("❌ 漫画删除失败: comicId={}", id);
        }
        return ResultUtils.success(result, "作品删除成功");
    }
}
