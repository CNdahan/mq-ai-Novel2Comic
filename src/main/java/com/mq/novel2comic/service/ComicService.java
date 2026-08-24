package com.mq.novel2comic.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mq.novel2comic.model.dto.comic.ComicListItemResponse;
import com.mq.novel2comic.model.dto.comic.ComicQueryRequest;
import com.mq.novel2comic.model.entity.Comic;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author MQ
* @description 针对表【comic(漫画作品表)】的数据库操作Service
* @createDate 2025-10-22 16:01:42
*/
public interface ComicService extends IService<Comic> {

    /**
     * 获取用户的漫画列表（分页）
     * @param request 查询请求
     * @param userId 用户ID
     * @return 分页结果
     */
    IPage<ComicListItemResponse> getUserComicList(ComicQueryRequest request, Long userId);
}
