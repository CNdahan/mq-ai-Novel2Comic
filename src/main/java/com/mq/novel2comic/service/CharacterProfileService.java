package com.mq.novel2comic.service;

import com.mq.novel2comic.model.entity.CharacterProfile;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author MQ
* @description 针对表【character_profile(角色档案表)】的数据库操作Service
* @createDate 2025-10-22 16:01:42
*/
public interface CharacterProfileService extends IService<CharacterProfile> {

    /**
     * 根据小说ID和角色名称查询角色档案
     * @param novelId 小说ID
     * @param characterName 角色名称
     * @return 角色档案
     */
    CharacterProfile getByNovelIdAndName(Long novelId, String characterName);
}
