package com.mq.novel2comic.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mq.novel2comic.model.dto.novel.*;
import com.mq.novel2comic.model.entity.Novel;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author MQ
* @description 针对表【novel(小说表)】的数据库操作Service
* @createDate 2025-10-21 15:30:52
*/
public interface NovelService extends IService<Novel> {

    /**
     * 解析小说文本
     * @param text 原始小说文本
     * @return 结构化结果
     */
    NovelStructure parseNovel(String text);

    /**
     * 上传小说
     * @param request 上传请求
     * @param userId 用户ID
     * @return 上传响应
     */
    NovelUploadResponse uploadNovel(NovelUploadRequest request, Long userId);

    /**
     * 获取小说详情
     * @param novelId 小说ID
     * @param userId 用户ID
     * @return 小说详情
     */
    NovelDetailResponse getNovelDetail(Long novelId, Long userId);

    /**
     * 获取用户小说列表
     * @param request 查询请求
     * @param userId 用户ID
     * @return 小说列表
     */
    IPage<NovelListItem> getUserNovelList(NovelQueryRequest request, Long userId);
}
