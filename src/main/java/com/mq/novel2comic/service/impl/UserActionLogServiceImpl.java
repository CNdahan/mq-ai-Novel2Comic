package com.mq.novel2comic.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mq.novel2comic.model.entity.UserActionLog;
import com.mq.novel2comic.mapper.UserActionLogMapper;
import com.mq.novel2comic.service.UserActionLogService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * UserActionLog Service实现
 * @author MQ
 */
@Service
public class UserActionLogServiceImpl extends ServiceImpl<UserActionLogMapper, UserActionLog>
        implements UserActionLogService {

    @Override
    public void log(Long userId, String actionType, String resourceType, Long resourceId, String ipAddress, String userAgent) {
        UserActionLog log = new UserActionLog();
        log.setUserId(userId);
        log.setActionType(actionType);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setCreateTime(new Date());
        this.save(log);
    }
}
