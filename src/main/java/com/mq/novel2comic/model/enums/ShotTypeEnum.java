package com.mq.novel2comic.model.enums;

import lombok.Getter;

/**
 * 镜头类型枚举
 * @author MQ
 */
@Getter
public enum ShotTypeEnum {
    CLOSE_UP("close_up", "特写"),
    MEDIUM("medium", "中景"),
    FULL("full", "全景"),
    LOW_ANGLE("low_angle", "仰视"),
    HIGH_ANGLE("high_angle", "俯视");

    private final String code;
    private final String desc;

    ShotTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ShotTypeEnum getByCode(String code) {
        for (ShotTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
