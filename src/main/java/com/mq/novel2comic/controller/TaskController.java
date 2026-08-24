package com.mq.novel2comic.controller;

import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.model.entity.GenerateTask;
import com.mq.novel2comic.service.GenerateTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 任务控制器
 * @author MQ
 */
@RestController
@RequestMapping("/task")
@Slf4j
public class TaskController {

    @Resource
    private GenerateTaskService generateTaskService;

    /**
     * 根据taskUuid获取任务进度
     */
    @GetMapping("/progress/{taskUuid}")
    public BaseResponse<GenerateTask> getTaskProgress(@PathVariable String taskUuid) {
        GenerateTask task = generateTaskService.getByTaskUuid(taskUuid);
        return ResultUtils.success(task);
    }

    /**
     * 取消任务
     */
    @PostMapping("/cancel/{taskUuid}")
    public BaseResponse<Boolean> cancelTask(@PathVariable String taskUuid) {
        generateTaskService.failTask(taskUuid, "用户取消任务");
        return ResultUtils.success(true);
    }
}
