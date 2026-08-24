package com.mq.novel2comic.model.dto.auth;

import lombok.Builder;
import lombok.Data;
import java.util.Date;

/**
 * VIP升级响应
 * @author MQ
 */
@Data
@Builder
public class VipUpgradeResponse {
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * VIP等级
     */
    private Integer vipLevel;
    
    /**
     * VIP过期时间
     */
    private Date vipExpireAt;
    
    /**
     * 新增配额
     */
    private Integer quotaAdded;
    
    /**
     * 当前剩余配额
     */
    private Integer quotaRemaining;
    
    /**
     * 支付状态：pending/completed/failed
     */
    private String paymentStatus;
    
    /**
     * 支付金额
     */
    private Double amount;
}

