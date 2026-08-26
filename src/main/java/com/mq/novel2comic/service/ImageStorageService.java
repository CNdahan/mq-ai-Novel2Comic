package com.mq.novel2comic.service;

/**
 * 图片存储服务接口
 * 用于将临时OSS URL的图片持久化保存
 * 
 * @author MQ
 */
public interface ImageStorageService {
    
    /**
     * 下载并保存图片
     * @param ossUrl OSS临时URL
     * @param comicId 漫画ID
     * @param panelIndex 分镜序号
     * @return 本地URL（永久有效）
     */
    String downloadAndSave(String ossUrl, Long comicId, int panelIndex);
    
    /**
     * 检查图片是否存在
     * @param comicId 漫画ID
     * @param panelIndex 分镜序号
     * @return 是否存在
     */
    boolean exists(Long comicId, int panelIndex);

    /**
     * 读取本地图片内容，用于批量下载和图片编排。
     */
    byte[] readImage(Long comicId, int panelIndex);
    
    /**
     * 获取本地图片URL
     * @param comicId 漫画ID
     * @param panelIndex 分镜序号
     * @return 本地URL
     */
    String getLocalUrl(Long comicId, int panelIndex);
    
    /**
     * 删除指定漫画的单个图片文件
     * @param comicId 漫画ID
     * @param panelIndex 分镜序号
     * @return 是否删除成功
     */
    boolean deleteImage(Long comicId, int panelIndex);
    
    /**
     * 删除指定漫画的所有图片文件
     * @param comicId 漫画ID
     * @param panelCount 分镜数量
     * @return 删除成功的文件数量
     */
    int deleteAllImages(Long comicId, int panelCount);
}

