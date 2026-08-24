package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.model.dto.image.CachedImage;
import com.mq.novel2comic.service.SemanticCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 语义缓存服务实现
 * 
 * 核心思路：
 * 1. 对每个Prompt进行向量化（Embedding）
 * 2. 计算新Prompt与缓存Prompt的余弦相似度
 * 3. 相似度 > 0.85 时命中缓存，直接返回已生成的图片
 * 4. 减少70%的API调用成本
 * 
 * 借鉴需求文档3.3节的设计
 * 
 * @author MQ
 */
@Service
@Slf4j
public class SemanticCacheServiceImpl implements SemanticCacheService {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 相似度阈值（借鉴需求文档中的配置）
     */
    private static final double SIMILARITY_THRESHOLD = 0.85;
    
    /**
     * 缓存键前缀
     */
    private static final String CACHE_KEY_PREFIX = "semantic:prompt:";
    
    /**
     * 缓存过期时间（6小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 6;
    
    /**
     * 检查语义缓存
     */
    @Override
    public Optional<CachedImage> checkCache(String prompt) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 1. 向量化查询Prompt
            List<Double> queryEmbedding = embedPrompt(prompt);
            
            // 2. 获取所有缓存的Prompt键
            Set<String> cacheKeys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (cacheKeys == null || cacheKeys.isEmpty()) {
                log.debug("语义缓存为空");
                return Optional.empty();
            }
            
            double maxSimilarity = 0.0;
            CachedImage bestMatch = null;
            
            // 3. 遍历计算相似度
            for (String cacheKey : cacheKeys) {
                Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
                if (cached == null) {
                    continue;
                }
                
                // 获取缓存的向量
                List<Double> cachedEmbedding = (List<Double>) cached.get("embedding");
                if (cachedEmbedding == null) {
                    continue;
                }
                
                // 计算余弦相似度
                double similarity = cosineSimilarity(queryEmbedding, cachedEmbedding);
                
                // 找到最相似的缓存
                if (similarity > SIMILARITY_THRESHOLD && similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    bestMatch = CachedImage.builder()
                            .imageUrl((String) cached.get("imageUrl"))
                            .similarity(similarity)
                            .originalPrompt((String) cached.get("prompt"))
                            .timestamp((Long) cached.get("timestamp"))
                            .build();
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (bestMatch != null) {
                log.info("✅ 语义缓存命中! 相似度={}, 耗时={}ms", 
                        String.format("%.4f", maxSimilarity), duration);
                return Optional.of(bestMatch);
            } else {
                log.debug("语义缓存未命中, 检查耗时={}ms", duration);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("检查语义缓存异常", e);
            return Optional.empty();
        }
    }
    
    /**
     * 缓存新生成的图片
     */
    @Override
    public void cacheImage(String prompt, String imageUrl) {
        try {
            // 1. 向量化Prompt
            List<Double> embedding = embedPrompt(prompt);
            
            // 2. 生成缓存键（使用MD5避免键过长）
            String cacheKey = CACHE_KEY_PREFIX + 
                    DigestUtils.md5DigestAsHex(prompt.getBytes());
            
            // 3. 构建缓存数据
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("embedding", embedding);
            cacheData.put("imageUrl", imageUrl);
            cacheData.put("prompt", prompt);
            cacheData.put("timestamp", System.currentTimeMillis());
            
            // 4. 存入Redis，设置6小时过期
            redisTemplate.opsForValue().set(
                    cacheKey, 
                    cacheData, 
                    CACHE_EXPIRE_HOURS, 
                    TimeUnit.HOURS
            );
            
            log.debug("语义缓存存储成功: key={}", cacheKey);
            
        } catch (Exception e) {
            log.error("缓存图片失败", e);
        }
    }
    
    /**
     * 清除过期缓存
     * Redis会自动过期，这里主要用于主动清理
     */
    @Override
    public void clearExpiredCache() {
        try {
            Set<String> cacheKeys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (cacheKeys == null || cacheKeys.isEmpty()) {
                return;
            }
            
            long now = System.currentTimeMillis();
            int deletedCount = 0;
            
            for (String cacheKey : cacheKeys) {
                Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    Long timestamp = (Long) cached.get("timestamp");
                    if (timestamp != null) {
                        long age = now - timestamp;
                        // 超过6小时的缓存删除
                        if (age > CACHE_EXPIRE_HOURS * 3600 * 1000) {
                            redisTemplate.delete(cacheKey);
                            deletedCount++;
                        }
                    }
                }
            }
            
            if (deletedCount > 0) {
                log.info("清除过期语义缓存: 数量={}", deletedCount);
            }
            
        } catch (Exception e) {
            log.error("清除过期缓存失败", e);
        }
    }
    
    /**
     * 对Prompt进行向量化
     */
    private List<Double> embedPrompt(String prompt) {
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(
                    Collections.singletonList(prompt)
            );
            
            if (response != null && !response.getResults().isEmpty()) {
                // 将float[]转为List<Double>便于存储
                float[] floatArray = response.getResults().get(0).getOutput();
                List<Double> doubleList = new ArrayList<>(floatArray.length);
                for (float v : floatArray) {
                    doubleList.add((double) v);
                }
                return doubleList;
            }
            
            throw new RuntimeException("Embedding结果为空");
            
        } catch (Exception e) {
            log.error("向量化Prompt失败: {}", prompt, e);
            throw new RuntimeException("向量化失败", e);
        }
    }
    
    /**
     * 计算余弦相似度
     * 公式: similarity = (A · B) / (||A|| * ||B||)
     * 
     * 借鉴需求文档3.3.2节的实现
     */
    private double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.size(); i++) {
            double a = vectorA.get(i);
            double b = vectorB.get(i);
            
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

