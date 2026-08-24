package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.novel.CharacterInfo;
import com.mq.novel2comic.model.entity.CharacterProfile;

import java.util.List;
import java.util.Optional;

/**
 * 角色一致性服务
 * 参考需求文档：02-功能需求详细设计.md 第2.4节
 * 参考文档：docs/角色一致性服务使用指南.md
 * 
 * 核心功能：
 * 1. 使用向量化存储角色特征
 * 2. 通过相似度检索保证角色一致性
 * 3. 三层缓存：内存 → 向量存储 → 数据库
 * 
 * @author MQ
 */
public interface CharacterConsistencyService {

    /**
     * 存储单个角色并向量化
     * 
     * @param novelId 小说ID
     * @param character 角色信息
     * @return 保存后的角色档案
     */
    CharacterProfile storeCharacter(Long novelId, CharacterInfo character);

    /**
     * 批量存储角色并向量化
     * 
     * @param novelId 小说ID
     * @param characters 角色列表
     * @return 保存后的角色档案列表
     */
    List<CharacterProfile> storeCharacters(Long novelId, List<CharacterInfo> characters);

    /**
     * 获取一致性角色描述（用于生成图片Prompt）
     * 核心方法！用于保证角色在不同分镜中的外观一致性
     * 
     * @param novelId 小说ID
     * @param characterName 角色名称
     * @return 角色的英文描述（用于AI绘画）
     */
    String getConsistentDescription(Long novelId, String characterName);

    /**
     * 获取角色档案（包含完整信息）
     * 
     * @param novelId 小说ID
     * @param characterName 角色名称
     * @return 角色档案
     */
    Optional<CharacterProfile> getCharacterProfile(Long novelId, String characterName);

    /**
     * 更新角色描述（用户编辑后）
     * 会重新向量化并更新存储
     * 
     * @param novelId 小说ID
     * @param characterName 角色名称
     * @param descriptionCn 中文描述
     * @param descriptionEn 英文描述
     * @return 是否更新成功
     */
    boolean updateCharacterDescription(Long novelId, String characterName, 
                                       String descriptionCn, String descriptionEn);

    /**
     * 搜索相似角色（用于去重或推荐）
     * 
     * @param novelId 小说ID
     * @param characterName 角色名称
     * @param topK 返回Top-K个最相似的角色
     * @return 相似角色列表（按相似度降序）
     */
    List<CharacterProfile> searchSimilarCharacters(Long novelId, String characterName, int topK);

    /**
     * 清除角色缓存
     * 
     * @param novelId 小说ID
     * @param characterName 角色名称（为空则清除该小说所有角色缓存）
     */
    void clearCache(Long novelId, String characterName);

    /**
     * 从CharacterInfo生成英文描述
     * （用于AI绘画的标准化Prompt）
     * 
     * @param character 角色信息
     * @return 英文描述
     */
    String generateEnglishDescription(CharacterInfo character);

    /**
     * 从CharacterInfo生成中文描述
     * （用于展示给用户）
     * 
     * @param character 角色信息
     * @return 中文描述
     */
    String generateChineseDescription(CharacterInfo character);
}

