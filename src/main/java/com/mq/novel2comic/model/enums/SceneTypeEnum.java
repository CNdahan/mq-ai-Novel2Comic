package com.mq.novel2comic.model.enums;

import lombok.Getter;

/**
 * 场景类型枚举
 * @author MQ
 */
@Getter
public enum SceneTypeEnum {
    DIALOGUE("dialogue", "对话场景"),
    ACTION("action", "动作场景"),
    DESCRIPTION("description", "描写场景"),
    EMOTION("emotion", "情绪场景");

    private final String code;
    private final String desc;

    SceneTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SceneTypeEnum getByCode(String code) {
        for (SceneTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
