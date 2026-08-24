package com.mq.novel2comic.model.enums;

import lombok.Getter;

/**
 * 镜头类型枚举
 * @author MQ
 */
@Getter
public enum ShotType {
    
    CLOSE_UP("close_up", "特写镜头", "close-up shot, focus on face or details"),
    
    MEDIUM("medium", "中景镜头", "medium shot, half body or full body"),
    
    FULL("full", "全景镜头", "full shot, wide angle, showing environment and characters"),
    
    LOW_ANGLE("low_angle", "仰视镜头", "low angle shot, looking up, dramatic perspective"),
    
    HIGH_ANGLE("high_angle", "俯视镜头", "high angle shot, bird's eye view, looking down");
    
    /**
     * 类型代码
     */
    private final String code;
    
    /**
     * 类型名称
     */
    private final String name;
    
    /**
     * Prompt描述
     */
    private final String promptDesc;
    
    ShotType(String code, String name, String promptDesc) {
        this.code = code;
        this.name = name;
        this.promptDesc = promptDesc;
    }
    
    /**
     * 根据代码获取类型
     */
    public static ShotType fromCode(String code) {
        for (ShotType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return MEDIUM; // 默认中景
    }
}

