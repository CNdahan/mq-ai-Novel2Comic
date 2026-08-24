package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.novel.CharacterInfo;
import com.mq.novel2comic.model.entity.CharacterProfile;
import com.mq.novel2comic.service.CharacterConsistencyService;
import com.mq.novel2comic.service.CharacterProfileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 角色一致性服务实现
 * 参考需求文档：02-功能需求详细设计.md 第2.4节
 * 参考文档：docs/角色一致性服务使用指南.md
 * 
 * 核心技术：
 * 1. 向量化存储：使用Spring AI的EmbeddingModel将角色描述向量化
 * 2. 相似度检索：余弦相似度阈值0.85（参考AI健身教练项目）
 * 3. 三层降级：L1缓存(Caffeine) → 向量存储(内存HashMap) → 数据库(MySQL)
 * 4. 自动同步：内存和数据库双写
 * 
 * @author MQ
 */
@Slf4j
@Service
public class CharacterConsistencyServiceImpl implements CharacterConsistencyService {

    @Resource
    private CharacterProfileService characterProfileService;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource(name = "characterCache")
    private Cache<String, CharacterProfile> caffeineCache;

    /**
     * 向量存储（内存HashMap）
     * Key: novelId:characterName
     * Value: 向量化的特征向量
     */
    private final ConcurrentHashMap<String, float[]> vectorStore = new ConcurrentHashMap<>();

    @Override
    public CharacterProfile storeCharacter(Long novelId, CharacterInfo character) {
        log.info("开始存储角色：novelId={}, characterName={}", novelId, character.getName());
        try {
            // 1. 生成中英文描述
            String descriptionEn = generateEnglishDescription(character);
            String descriptionCn = generateChineseDescription(character);
            // 2. 向量化英文描述（用于相似度检索）
            float[] embedding = null;
            try {
                embedding = embedDescription(descriptionEn);
            } catch (BusinessException e) {
                log.warn("角色向量化失败，降级为仅文本存储：novelId={}, characterName={}, reason={}",
                        novelId, character.getName(), e.getMessage());
            }
            // 3. 检查是否已存在
            QueryWrapper<CharacterProfile> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("novelId", novelId)
                       .eq("characterName", character.getName());
            CharacterProfile existingProfile = characterProfileService.getOne(queryWrapper);
            CharacterProfile profile;
            if (existingProfile != null) {
                // 更新现有角色
                existingProfile.setDescriptionCn(descriptionCn);
                existingProfile.setDescriptionEn(descriptionEn);
                // appearanceData 使用 JacksonTypeHandler 自动序列化，直接设置对象即可
                existingProfile.setAppearanceData(character.getAppearance());
                if (embedding != null) {
                    existingProfile.setEmbeddingVector(embedding);
                }
                characterProfileService.updateById(existingProfile);
                profile = existingProfile;
                log.info("更新已存在的角色：{}", character.getName());
            } else {
                // 创建新角色
                profile = new CharacterProfile();
                profile.setNovelId(novelId);
                profile.setCharacterName(character.getName());
                profile.setDescriptionCn(descriptionCn);
                profile.setDescriptionEn(descriptionEn);
                // appearanceData 使用 JacksonTypeHandler 自动序列化，直接设置对象即可
                profile.setAppearanceData(character.getAppearance());
                if (embedding != null) {
                    profile.setEmbeddingVector(embedding);
                }
                profile.setUseCount(0);
                characterProfileService.save(profile);
                log.info("创建新角色：{}", character.getName());
            }
            // 4. 存入向量存储（内存）
            if (embedding != null) {
                String vectorKey = buildVectorKey(novelId, character.getName());
                vectorStore.put(vectorKey, embedding);
            }
            // 5. 存入L1缓存
            String cacheKey = buildCacheKey(novelId, character.getName());
            caffeineCache.put(cacheKey, profile);
            log.info("角色存储完成：novelId={}, characterName={}, embedding维度={}", 
                    novelId, character.getName(), embedding != null ? embedding.length : 0);
            return profile;
        } catch (Exception e) {
            log.error("存储角色失败：novelId={}, characterName={}", novelId, character.getName(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "存储角色失败: " + e.getMessage());
        }
    }

    @Override
    public List<CharacterProfile> storeCharacters(Long novelId, List<CharacterInfo> characters) {
        log.info("开始批量存储角色：novelId={}, 数量={}", novelId, characters.size());
        List<CharacterProfile> profiles = new ArrayList<>();
        for (CharacterInfo character : characters) {
            try {
                CharacterProfile profile = storeCharacter(novelId, character);
                profiles.add(profile);
            } catch (Exception e) {
                log.error("存储角色失败，跳过：{}", character.getName(), e);
                // 继续处理其他角色
            }
        }
        log.info("批量存储完成：成功{}/{}", profiles.size(), characters.size());
        return profiles;
    }

    @Override
    public String getConsistentDescription(Long novelId, String characterName) {
        log.debug("获取一致性描述：novelId={}, characterName={}", novelId, characterName);
        // 三层缓存降级策略
        // 1. 尝试L1缓存（Caffeine）
        String cacheKey = buildCacheKey(novelId, characterName);
        CharacterProfile cachedProfile = caffeineCache.getIfPresent(cacheKey);
        if (cachedProfile != null) {
            log.debug("L1缓存命中：{}", characterName);
            return cachedProfile.getDescriptionEn();
        }
        // 2. 尝试向量存储（内存HashMap，带相似度检索）
        String vectorKey = buildVectorKey(novelId, characterName);
        if (vectorStore.containsKey(vectorKey)) {
            // 从数据库查询完整信息
            Optional<CharacterProfile> profileOpt = getCharacterProfile(novelId, characterName);
            if (profileOpt.isPresent()) {
                CharacterProfile profile = profileOpt.get();
                // 回填L1缓存
                caffeineCache.put(cacheKey, profile);
                log.debug("向量存储命中：{}", characterName);
                return profile.getDescriptionEn();
            }
        }

        // 3. 降级到数据库查询
        Optional<CharacterProfile> profileOpt = getCharacterProfile(novelId, characterName);
        if (profileOpt.isPresent()) {
            CharacterProfile profile = profileOpt.get();
            // 回填缓存和向量存储
            caffeineCache.put(cacheKey, profile);
            if (profile.getEmbeddingVector() != null) {
                try {
                    // embeddingVector 已经被 JacksonTypeHandler 自动反序列化
                    float[] embedding = (float[]) profile.getEmbeddingVector();
                    vectorStore.put(vectorKey, embedding);
                } catch (Exception e) {
                    log.warn("回填向量存储失败", e);
                }
            }
            log.debug("数据库查询命中：{}", characterName);
            return profile.getDescriptionEn();
        }
        // 4. 未找到角色
        log.warn("未找到角色：novelId={}, characterName={}", novelId, characterName);
        return "";
    }

    @Override
    public Optional<CharacterProfile> getCharacterProfile(Long novelId, String characterName) {
        QueryWrapper<CharacterProfile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("novelId", novelId)
                   .eq("characterName", characterName)
                   .eq("isDelete", 0);
        CharacterProfile profile = characterProfileService.getOne(queryWrapper);
        return Optional.ofNullable(profile);
    }

    @Override
    public boolean updateCharacterDescription(Long novelId, String characterName, 
                                             String descriptionCn, String descriptionEn) {
        log.info("更新角色描述：novelId={}, characterName={}", novelId, characterName);

        try {
            // 1. 查询角色
            Optional<CharacterProfile> profileOpt = getCharacterProfile(novelId, characterName);
            if (profileOpt.isEmpty()) {
                log.warn("角色不存在：{}", characterName);
                return false;
            }
            CharacterProfile profile = profileOpt.get();
            // 2. 更新描述
            profile.setDescriptionCn(descriptionCn);
            profile.setDescriptionEn(descriptionEn);
            // 3. 重新向量化
            float[] embedding = null;
            try {
                embedding = embedDescription(descriptionEn);
            } catch (BusinessException e) {
                log.warn("角色向量化失败，保留文本更新：novelId={}, characterName={}, reason={}",
                        novelId, characterName, e.getMessage());
            }
            // embeddingVector 使用 JacksonTypeHandler 自动序列化，直接设置对象即可
            if (embedding != null) {
                profile.setEmbeddingVector(embedding);
            }
            // 4. 保存到数据库
            characterProfileService.updateById(profile);
            // 5. 更新向量存储
            if (embedding != null) {
                String vectorKey = buildVectorKey(novelId, characterName);
                vectorStore.put(vectorKey, embedding);
            }
            // 6. 清除缓存（让其重新加载）
            clearCache(novelId, characterName);
            log.info("角色描述更新成功：{}", characterName);
            return true;
        } catch (Exception e) {
            log.error("更新角色描述失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新角色描述失败: " + e.getMessage());
        }
    }

    @Override
    public List<CharacterProfile> searchSimilarCharacters(Long novelId, String characterName, int topK) {
        log.info("搜索相似角色：novelId={}, characterName={}, topK={}", novelId, characterName, topK);
        try {
            // 1. 获取目标角色的向量
            String targetVectorKey = buildVectorKey(novelId, characterName);
            float[] targetEmbedding = vectorStore.get(targetVectorKey);
            if (targetEmbedding == null) {
                // 尝试从数据库加载
                Optional<CharacterProfile> profileOpt = getCharacterProfile(novelId, characterName);
                if (profileOpt.isPresent() && profileOpt.get().getEmbeddingVector() != null) {
                    // embeddingVector 已经被 JacksonTypeHandler 自动反序列化
                    targetEmbedding = (float[]) profileOpt.get().getEmbeddingVector();
                    vectorStore.put(targetVectorKey, targetEmbedding);
                } else {
                    log.warn("角色向量不存在：{}", characterName);
                    return Collections.emptyList();
                }
            }

            // 2. 计算与所有角色的相似度
            List<SimilarityResult> results = new ArrayList<>();
            for (Map.Entry<String, float[]> entry : vectorStore.entrySet()) {
                String key = entry.getKey();
                // 跳过自己
                if (key.equals(targetVectorKey)) {
                    continue;
                }
                // 只比较同一小说的角色
                if (!key.startsWith(novelId + ":")) {
                    continue;
                }
                float[] embedding = entry.getValue();
                double similarity = cosineSimilarity(targetEmbedding, embedding);
                // 提取角色名
                String name = key.substring(key.indexOf(":") + 1);
                results.add(new SimilarityResult(name, similarity));
            }
            // 3. 按相似度降序排序，取Top-K
            List<String> topCharacterNames = results.stream()
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(topK)
                .map(r -> r.characterName)
                .toList();
            // 4. 查询完整角色信息
            if (topCharacterNames.isEmpty()) {
                return Collections.emptyList();
            }
            QueryWrapper<CharacterProfile> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("novelId", novelId)
                       .in("characterName", topCharacterNames)
                       .eq("isDelete", 0);
            List<CharacterProfile> profiles = characterProfileService.list(queryWrapper);
            log.info("找到{}个相似角色", profiles.size());
            return profiles;
        } catch (Exception e) {
            log.error("搜索相似角色失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void clearCache(Long novelId, String characterName) {
        if (StrUtil.isBlank(characterName)) {
            // 清除该小说的所有角色缓存
            log.info("清除小说所有角色缓存：novelId={}", novelId);
            caffeineCache.asMap().keySet().removeIf(key -> key.startsWith(novelId + ":"));
        } else {
            // 清除指定角色缓存
            String cacheKey = buildCacheKey(novelId, characterName);
            caffeineCache.invalidate(cacheKey);
            log.info("清除角色缓存：{}", cacheKey);
        }
    }

    @Override
    public String generateEnglishDescription(CharacterInfo character) {
        StringBuilder desc = new StringBuilder();
        // 基本信息
        if (StrUtil.isNotBlank(character.getRole())) {
            String roleDesc = switch (character.getRole()) {
                case "protagonist" -> "main character";
                case "supporting" -> "supporting character";
                case "minor" -> "minor character";
                default -> "character";
            };
            desc.append("A ").append(roleDesc).append(", ");
        }
        // 描述
        if (StrUtil.isNotBlank(character.getDescription())) {
            desc.append(character.getDescription()).append(", ");
        }
        // 外貌特征
        if (character.getAppearance() != null && !character.getAppearance().isEmpty()) {
            desc.append(String.join(", ", character.getAppearance())).append(", ");
        }
        // 性格特点（可选，用于风格控制）
        if (character.getPersonality() != null && !character.getPersonality().isEmpty()) {
            String personalities = String.join(" and ", character.getPersonality());
            desc.append("with ").append(personalities).append(" personality");
        }
        String result = desc.toString().trim();
        // 移除末尾的逗号
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    @Override
    public String generateChineseDescription(CharacterInfo character) {
        StringBuilder desc = new StringBuilder();
        // 角色类型
        if (StrUtil.isNotBlank(character.getRole())) {
            String roleDesc = switch (character.getRole()) {
                case "protagonist" -> "主角";
                case "supporting" -> "配角";
                case "minor" -> "次要角色";
                default -> "角色";
            };
            desc.append(roleDesc).append("，");
        }
        // 描述
        if (StrUtil.isNotBlank(character.getDescription())) {
            desc.append(character.getDescription()).append("。");
        }
        // 外貌特征
        if (character.getAppearance() != null && !character.getAppearance().isEmpty()) {
            desc.append("外貌特征：").append(String.join("、", character.getAppearance())).append("。");
        }
        // 性格特点
        if (character.getPersonality() != null && !character.getPersonality().isEmpty()) {
            desc.append("性格特点：").append(String.join("、", character.getPersonality())).append("。");
        }
        return desc.toString();
    }

    /**
     * 向量化文本描述
     * 使用Spring AI的EmbeddingModel
     */
    private float[] embedDescription(String description) {
        try {
            if (StrUtil.isBlank(description)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述内容不能为空");
            }
            log.debug("开始向量化：{}", description.substring(0, Math.min(50, description.length())));
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(description));
            if (response == null || response.getResults().isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "向量化失败：响应为空");
            }
            float[] embedding = response.getResults().get(0).getOutput();
            log.debug("向量化完成，维度：{}", embedding.length);
            return embedding;
        } catch (Exception e) {
            log.error("向量化失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "向量化失败: " + e.getMessage());
        }
    }

    /**
     * 计算余弦相似度
     * 借鉴AI健身教练项目中已验证的算法
     * 
     * @param a 向量A
     * @param b 向量B
     * @return 余弦相似度 [0, 1]
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配：" + a.length + " vs " + b.length);
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 构建缓存Key
     */
    private String buildCacheKey(Long novelId, String characterName) {
        return novelId + ":" + characterName;
    }

    /**
     * 构建向量存储Key
     */
    private String buildVectorKey(Long novelId, String characterName) {
        return novelId + ":" + characterName;
    }

    /**
     * 相似度结果内部类
     */
    private static class SimilarityResult {
        String characterName;
        double similarity;
        SimilarityResult(String characterName, double similarity) {
            this.characterName = characterName;
            this.similarity = similarity;
        }
    }
}

