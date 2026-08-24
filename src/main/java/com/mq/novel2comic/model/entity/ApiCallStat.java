package com.mq.novel2comic.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * API调用统计表
 * @TableName api_call_stat
 */
@TableName(value ="api_call_stat")
@Data
public class ApiCallStat {
    /**
     * 统计ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * API类型
     */
    private String apiType;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 请求Token数
     */
    private Integer requestTokens;

    /**
     * 响应Token数
     */
    private Integer responseTokens;

    /**
     * 图片数量
     */
    private Integer imageCount;

    /**
     * 成本金额
     */
    private BigDecimal costAmount;

    /**
     * 响应时间(ms)
     */
    private Integer responseTimeMs;

    /**
     * 是否成功
     */
    private Integer isSuccess;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}
