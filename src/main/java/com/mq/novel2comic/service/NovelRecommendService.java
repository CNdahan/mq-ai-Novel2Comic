package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.novel.NovelRecommendDTO;
import java.util.List;

/**
 * 小说推荐服务接口
 */
public interface NovelRecommendService {

    /**
     * 获取每日推荐小说（随机3篇）
     * @return 推荐小说列表
     */
    List<NovelRecommendDTO> getDailyRecommendations();
}

