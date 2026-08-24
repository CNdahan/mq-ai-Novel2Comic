package com.mq.novel2comic.service;

import com.mq.novel2comic.model.entity.UserActionLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * UserActionLog Service
 * @author MQ
 */
public interface UserActionLogService extends IService<UserActionLog> {

    /**
     * 记录用户操作日志
     */
    void log(Long userId, String actionType, String resourceType, Long resourceId, String ipAddress, String userAgent);
}
