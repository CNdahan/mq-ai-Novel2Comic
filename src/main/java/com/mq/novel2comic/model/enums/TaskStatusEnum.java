package com.mq.novel2comic.model.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 * @author MQ
 */
@Getter
public enum TaskStatusEnum {
    PENDING("pending", "待处理"),
    PROCESSING("processing", "处理中"),
    COMPLETED("completed", "已完成"),
    FAILED("failed", "失败");

    private final String code;
    private final String desc;

    TaskStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskStatusEnum getByCode(String code) {
        for (TaskStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
