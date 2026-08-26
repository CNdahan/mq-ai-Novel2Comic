package com.mq.novel2comic.service;

/**
 * 进度通知服务
 * 通过WebSocket实时推送任务进度
 * 
 * @author MQ
 */
public interface ProgressNotifyService {
    
    /**
     * 推送任务进度
     * @param taskId 任务ID
     * @param currentPanel 当前分镜序号
     * @param totalPanels 总分镜数
     * @param status 当前状态
     */
    void notifyProgress(String taskId, int currentPanel, int totalPanels, String status);
    
    /**
     * 推送任务完成
     * @param taskId 任务ID
     * @param comicId 生成的漫画ID
     */
    void notifyCompleted(String taskId, Long comicId);

    /** 推送分镜生成任务完成 */
    void notifyStoryboardCompleted(String taskId, Long novelId);
    
    /**
     * 推送任务失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void notifyFailed(String taskId, String errorMessage);
}

