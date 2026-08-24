package com.mq.novel2comic.model.enums;

import lombok.Getter;

/**
 * 漫画风格枚举
 * @author MQ
 */
@Getter
public enum ComicStyleEnum {
    JAPANESE("japanese", "日式漫画"),
    CHINESE("chinese", "国风漫画"),
    REALISTIC("realistic", "写实风格");

    private final String code;
    private final String desc;

    ComicStyleEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ComicStyleEnum getByCode(String code) {
        for (ComicStyleEnum style : values()) {
            if (style.getCode().equals(code)) {
                return style;
            }
        }
        return null;
    }
}
