package com.mq.novel2comic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mq.novel2comic.model.entity.CharacterProfile;
import com.mq.novel2comic.service.CharacterProfileService;
import com.mq.novel2comic.mapper.CharacterProfileMapper;
import org.springframework.stereotype.Service;

/**
* @author MQ
* @description 针对表【character_profile(角色档案表)】的数据库操作Service实现
* @createDate 2025-10-22 16:01:42
*/
@Service
public class CharacterProfileServiceImpl extends ServiceImpl<CharacterProfileMapper, CharacterProfile>
    implements CharacterProfileService{

    /**
     * 根据小说ID和角色名称查询角色档案
     */
    @Override
    public CharacterProfile getByNovelIdAndName(Long novelId, String characterName) {
        LambdaQueryWrapper<CharacterProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CharacterProfile::getNovelId, novelId)
               .eq(CharacterProfile::getCharacterName, characterName)
               .eq(CharacterProfile::getIsDelete, 0);
        return this.getOne(wrapper);
    }
}




