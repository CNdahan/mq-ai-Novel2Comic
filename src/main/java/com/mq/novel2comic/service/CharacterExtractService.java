package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.novel.CharacterInfo;
import com.mq.novel2comic.model.entity.CharacterProfile;

import java.util.List;

/**
 * 角色提取服务接口
 */
public interface CharacterExtractService {

    /**
     * 提取角色信息
     * @param text 小说文本
     * @return 角色列表
     */
    List<CharacterInfo> extract(String text);

    /**
     * 提取角色并自动存储
     * 完整流程：提取 → 向量化 → 存储到数据库和向量库
     * 
     * @param novelId 小说ID
     * @param text 小说文本
     * @return 存储后的角色档案列表
     */
    List<CharacterProfile> extractAndStore(Long novelId, String text);
}
