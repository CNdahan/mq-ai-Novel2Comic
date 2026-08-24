package com.mq.novel2comic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mq.novel2comic.model.entity.GenerateTask;
import com.mq.novel2comic.mapper.GenerateTaskMapper;
import com.mq.novel2comic.service.GenerateTaskService;
import com.mq.novel2comic.service.ProgressNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;

/**
 * GenerateTask Service实现
 * @author MQ
 */
@Service
@Slf4j
public class GenerateTaskServiceImpl extends ServiceImpl<GenerateTaskMapper, GenerateTask>
        implements GenerateTaskService {

    @Resource
    private ProgressNotifyService progressNotifyService;

    @Override
    public GenerateTask getByTaskUuid(String taskUuid) {
        QueryWrapper<GenerateTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("taskUuid", taskUuid);
        return this.getOne(queryWrapper);
    }

    @Override
    public void updateProgress(String taskUuid, Integer progress, String currentStep) {
        GenerateTask task = getByTaskUuid(taskUuid);
        if (task != null) {
            task.setProgressPercent(progress);
            task.setCurrentStep(currentStep);
            task.setUpdateTime(new Date());
            this.updateById(task);
            
            // 推送进度更新
            int totalPanels = task.getTotalPanels() != null ? task.getTotalPanels() : 0;
            int completedPanels = task.getCompletedPanels() != null ? task.getCompletedPanels() : 0;
            progressNotifyService.notifyProgress(taskUuid, completedPanels, totalPanels, currentStep);
        }
    }

    @Override
    public void completeTask(String taskUuid) {
        GenerateTask task = getByTaskUuid(taskUuid);
        if (task != null) {
            task.setStatus("completed");
            task.setProgressPercent(100);
            task.setCompleteTime(new Date());
            task.setUpdateTime(new Date());
            this.updateById(task);
            
            log.info("任务完成，准备推送WebSocket通知: taskUuid={}", taskUuid);
        }
    }

    @Override
    public void failTask(String taskUuid, String errorMessage) {
        GenerateTask task = getByTaskUuid(taskUuid);
        if (task != null) {
            task.setStatus("failed");
            task.setErrorMessage(errorMessage);
            task.setCompleteTime(new Date());
            task.setUpdateTime(new Date());
            this.updateById(task);
            
            // 推送失败通知
            progressNotifyService.notifyFailed(taskUuid, errorMessage);
        }
    }
}
