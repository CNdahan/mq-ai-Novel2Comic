package com.mq.novel2comic.model.dto.novel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 场景信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneInfo {

    /**
     * 场景编号
     */
    private Integer sceneNumber;

    /**
     * 场景内容
     */
    private String content;

    /**
     * 场景地点
     */
    private String location;

    /**
     * 场景时间
     */
    private String time;

    /**
     * 参与角色
     */
    private List<String> characters;

    /**
     * 场景描述
     */
    private String description;
}
