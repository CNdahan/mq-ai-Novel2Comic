package com.mq.novel2comic.model.dto.auth;

import lombok.Data;

/**
 * VIP升级请求
 * @author MQ
 */
@Data
public class VipUpgradeRequest {
    
    /**
     * VIP等级：1-月费，2-年费
     */
    private Integer vipLevel;
    
    /**
     * 购买时长（月）
     */
    private Integer duration;
    
    /**
     * 支付方式（可选）：alipay/wechat/mock
     */
    private String paymentMethod;
}

