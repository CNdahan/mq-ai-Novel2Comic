package com.mq.novel2comic.service;

import com.mq.novel2comic.model.entity.StoryboardPanel;
import java.util.List;

/**
 * 分镜版本管理服务
 * 支持多版本分镜生成和管理
 * 
 * @author MQ
 */
public interface StoryboardVersionService {

    /**
     * 获取小说的下一个可用版本号
     * 
     * @param novelId 小说ID
     * @return 下一个版本号（从1开始）
     */
    Integer getNextVersion(Long novelId);

    /**
     * 获取小说的当前版本号
     * 
     * @param novelId 小说ID
     * @return 当前版本号，如果没有则返回0
     */
    Integer getCurrentVersion(Long novelId);

    /**
     * 设置某个版本为当前版本
     * 
     * @param novelId 小说ID
     * @param version 要设置的版本号
     * @return 是否成功
     */
    boolean setCurrentVersion(Long novelId, Integer version);

    /**
     * 获取小说的所有版本列表
     * 
     * @param novelId 小说ID
     * @return 版本号列表（降序）
     */
    List<Integer> getAllVersions(Long novelId);

    /**
     * 删除指定版本的分镜
     * 
     * @param novelId 小说ID
     * @param version 版本号
     * @return 是否成功
     */
    boolean deleteVersion(Long novelId, Integer version);

    /**
     * 获取指定版本的所有分镜
     * 
     * @param novelId 小说ID
     * @param version 版本号，如果为null则获取当前版本
     * @return 分镜列表
     */
    List<StoryboardPanel> getVersionPanels(Long novelId, Integer version);

    /**
     * 复制一个版本的分镜到新版本
     * 
     * @param novelId 小说ID
     * @param sourceVersion 源版本号
     * @param targetVersion 目标版本号
     * @return 是否成功
     */
    boolean copyVersion(Long novelId, Integer sourceVersion, Integer targetVersion);
}

