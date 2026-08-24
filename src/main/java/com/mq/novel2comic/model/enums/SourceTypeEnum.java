package com.mq.novel2comic.model.enums;

import lombok.Getter;

/**
 * 来源类型枚举
 * @author MQ
 */
@Getter
public enum SourceTypeEnum {
    DIRECT("direct", "直接输入"),
    FILE("file", "文件上传"),
    URL("url", "URL导入");

    private final String code;
    private final String desc;

    SourceTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SourceTypeEnum getByCode(String code) {
        for (SourceTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
