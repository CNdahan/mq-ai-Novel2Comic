package com.mq.novel2comic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.exception.ThrowUtils;
import com.mq.novel2comic.model.dto.comic.ComicListItemResponse;
import com.mq.novel2comic.model.dto.comic.ComicQueryRequest;
import com.mq.novel2comic.model.entity.Comic;
import com.mq.novel2comic.model.entity.ComicPanel;
import com.mq.novel2comic.model.entity.Novel;
import com.mq.novel2comic.service.ComicPanelService;
import com.mq.novel2comic.service.ComicService;
import com.mq.novel2comic.mapper.ComicMapper;
import com.mq.novel2comic.service.NovelService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author MQ
* @description 针对表【comic(漫画作品表)】的数据库操作Service实现
* @createDate 2025-10-22 16:01:42
*/
@Service
public class ComicServiceImpl extends ServiceImpl<ComicMapper, Comic>
    implements ComicService{

    @Resource
    private NovelService novelService;
    
    @Resource
    private ComicPanelService comicPanelService;

    @Override
    public IPage<ComicListItemResponse> getUserComicList(ComicQueryRequest request, Long userId) {
        // 参数校验
        int current = request.getCurrent();
        int pageSize = request.getPageSize();
        ThrowUtils.throwIf(current <= 0, ErrorCode.PARAMS_ERROR, "页码必须大于0");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "每页数量必须在1-50之间");
        // 构建查询条件
        LambdaQueryWrapper<Comic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comic::getUserId, userId);
        queryWrapper.eq(Comic::getIsDelete, 0);
        // 状态筛选
        String status = request.getStatus();
        if (status != null && !"all".equals(status)) {
            queryWrapper.eq(Comic::getStatus, status);
        }
        // 风格筛选
        String style = request.getStyle();
        if (style != null && !"all".equals(style)) {
            queryWrapper.eq(Comic::getStyle, style);
        }
        // 小说ID筛选
        if (request.getNovelId() != null) {
            queryWrapper.eq(Comic::getNovelId, request.getNovelId());
        }
        // 按创建时间降序
        queryWrapper.orderByDesc(Comic::getCreateTime);
        // 分页查询
        Page<Comic> page = new Page<>(current, pageSize);
        Page<Comic> comicPage = this.page(page, queryWrapper);
        // 如果查询结果为空，直接返回空的分页结果
        if (comicPage.getRecords().isEmpty()) {
            Page<ComicListItemResponse> emptyPage = new Page<>(comicPage.getCurrent(), comicPage.getSize(), comicPage.getTotal());
            emptyPage.setRecords(new java.util.ArrayList<>());
            return emptyPage;
        }
        // 获取小说信息（批量查询优化）
        List<Long> novelIds = comicPage.getRecords().stream()
                .map(Comic::getNovelId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Novel> novelMap = novelService.listByIds(novelIds).stream()
                .collect(Collectors.toMap(Novel::getId, novel -> novel));
        // 获取每个漫画的封面图（第一幅分镜的图片）
        // 注意：使用 HashMap 手动收集，因为 Collectors.toMap 不允许 null 值
        Map<Long, String> coverMap = new java.util.HashMap<>();
        for (Comic comic : comicPage.getRecords()) {
            coverMap.put(comic.getId(), getFirstPanelImage(comic.getId()));
        }
        // 转换为DTO
        Page<ComicListItemResponse> resultPage = new Page<>(comicPage.getCurrent(), comicPage.getSize(), comicPage.getTotal());
        List<ComicListItemResponse> items = comicPage.getRecords().stream()
                .map(comic -> {
                    Novel novel = novelMap.get(comic.getNovelId());
                    return ComicListItemResponse.builder()
                            .comicId(comic.getId())
                            .title(comic.getComicTitle())
                            .novelId(comic.getNovelId())
                            .novelTitle(novel != null ? novel.getNovelTitle() : "未知小说")
                            .style(comic.getStyle())
                            .status(comic.getStatus())
                            .panelCount(comic.getPanelCount())
                            .coverImage(coverMap.get(comic.getId()))
                            .createdAt(convertToLocalDateTime(comic.getCreateTime()))
                            .updatedAt(convertToLocalDateTime(comic.getUpdateTime()))
                            .build();
                })
                .collect(Collectors.toList());
        resultPage.setRecords(items);
        return resultPage;
    }
    
    /**
     * 获取漫画的第一幅分镜图片
     */
    private String getFirstPanelImage(Long comicId) {
        QueryWrapper<ComicPanel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("comicId", comicId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByAsc("panelIndex");
        queryWrapper.last("LIMIT 1");
        ComicPanel firstPanel = comicPanelService.getOne(queryWrapper);
        return firstPanel != null ? firstPanel.getImageUrl() : null;
    }
    
    /**
     * Date转LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}




