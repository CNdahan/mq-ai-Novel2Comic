package com.mq.novel2comic.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mq.novel2comic.model.entity.CharacterProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 参考需求文档：04-数据库设计.md 第3节
 * 借鉴Dada项目的双层缓存经验（260ms→60ms）
 * 
 * @author MQ
 */
@Configuration
public class CacheConfig {

    /**
     * L1缓存：Caffeine（本地缓存）
     * 用于角色档案的高速缓存
     * 
     * 配置参考Dada项目验证过的参数：
     * - maximumSize: 1000个对象
     * - expireAfterWrite: 30分钟
     * - recordStats: 记录统计信息（用于监控）
     */
    @Bean(name = "characterCache")
    public Cache<String, CharacterProfile> characterCache() {
        return Caffeine.newBuilder()
                // 最多缓存1000个角色
                .maximumSize(1000)
                // 写入后30分钟过期
                .expireAfterWrite(30, TimeUnit.MINUTES)
                // 记录统计信息（命中率、加载时间等）
                .recordStats()
                .build();
    }

    /**
     * 通用对象缓存（可选）
     * 用于缓存其他常用数据
     */
    @Bean(name = "commonCache")
    public Cache<String, Object> commonCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}

