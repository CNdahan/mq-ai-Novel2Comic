package com.mq.novel2comic.model.dto.novel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 角色信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterInfo {

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 外貌特征
     */
    private List<String> appearance;

    /**
     * 性格特点
     */
    private List<String> personality;

    /**
     * 角色类型（protagonist/supporting/minor）
     */
    private String role;
}
