package com.mq.novel2comic.model.enums;

import lombok.Getter;

/**
 * 漫画风格枚举
 * @author MQ
 */
@Getter
public enum ComicStyle {
    
    JAPANESE("japanese", "日式漫画", "<anime>", "Japanese manga style, black and white, screentone shading, detailed line art"),
    
    CHINESE("chinese", "国风漫画", "<3d cartoon>", "Chinese manhua style, colored, dynamic composition, vibrant colors"),
    
    REALISTIC("realistic", "写实风格", "<flat illustration>", "Semi-realistic illustration, detailed, cinematic lighting, photorealistic");
    
    /**
     * 风格代码
     */
    private final String code;
    
    /**
     * 风格名称
     */
    private final String name;
    
    /**
     * 通义万相风格参数
     */
    private final String wanxStyle;
    
    /**
     * Prompt风格描述
     */
    private final String stylePrompt;
    
    ComicStyle(String code, String name, String wanxStyle, String stylePrompt) {
        this.code = code;
        this.name = name;
        this.wanxStyle = wanxStyle;
        this.stylePrompt = stylePrompt;
    }
    
    /**
     * 根据代码获取风格
     */
    public static ComicStyle fromCode(String code) {
        for (ComicStyle style : values()) {
            if (style.getCode().equals(code)) {
                return style;
            }
        }
        return JAPANESE; // 默认日式风格
    }
}

