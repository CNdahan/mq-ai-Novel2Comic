package com.mq.novel2comic.service;

import com.mq.novel2comic.model.entity.GenerateTask;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * GenerateTask Service
 * @author MQ
 */
public interface GenerateTaskService extends IService<GenerateTask> {

    /**
     * 根据taskUuid获取任务
     */
    GenerateTask getByTaskUuid(String taskUuid);

    /**
     * 更新任务进度
     */
    void updateProgress(String taskUuid, Integer progress, String currentStep);

    /**
     * 标记任务完成
     */
    void completeTask(String taskUuid);

    /**
     * 标记任务失败
     */
    void failTask(String taskUuid, String errorMessage);
}
