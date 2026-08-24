package com.mq.novel2comic.model.enums;

import lombok.Getter;

/**
 * API类型枚举
 * @author MQ
 */
@Getter
public enum ApiTypeEnum {
    LLM("llm", "大语言模型"),
    IMAGE("image", "图像生成"),
    EMBEDDING("embedding", "向量化");

    private final String code;
    private final String desc;

    ApiTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ApiTypeEnum getByCode(String code) {
        for (ApiTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
