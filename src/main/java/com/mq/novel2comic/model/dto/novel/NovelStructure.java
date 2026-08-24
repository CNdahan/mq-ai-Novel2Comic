package com.mq.novel2comic.model.dto.novel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 小说结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovelStructure {

    /**
     * 小说ID
     */
    private Long novelId;

    /**
     * 小说标题
     */
    private String title;

    /**
     * 角色列表
     */
    private List<CharacterInfo> characters;

    /**
     * 场景列表
     */
    private List<SceneInfo> scenes;

    /**
     * 故事概要
     */
    private String summary;

    /**
     * 故事主题
     */
    private String theme;
}
