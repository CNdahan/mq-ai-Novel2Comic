package com.mq.novel2comic.model.dto.novel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分镜面板
 * 表示漫画的单个分镜格
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardPanel {

    /**
     * 分镜ID
     */
    private String id;

    /**
     * 分镜序号
     */
    private Integer index;

    /**
     * 场景类型（dialogue-对话, action-动作, environment-环境描写, climax-高潮）
     */
    private String sceneType;

    /**
     * 镜头类型（close-up特写, medium中景, full全景, low-angle仰视, high-angle俯视）
     */
    private String shotType;

    /**
     * 参与角色列表
     */
    private List<String> characters;

    /**
     * 环境描述
     */
    private String environment;

    /**
     * 情绪氛围（bright-明亮, dark-阴暗, tense-紧张, warm-温馨）
     */
    private String mood;

    /**
     * 场景原文
     */
    private String originalText;

    /**
     * 基础场景描述（中文）
     */
    private String descriptionCn;

    /**
     * 英文绘画Prompt（用于AI生成图片）
     */
    private String prompt;
}
