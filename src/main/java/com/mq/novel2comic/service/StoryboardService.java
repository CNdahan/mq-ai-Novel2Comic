package com.mq.novel2comic.service;

import com.mq.novel2comic.model.dto.novel.NovelStructure;
import com.mq.novel2comic.model.dto.novel.SceneInfo;
import com.mq.novel2comic.model.dto.novel.StoryboardPanel;

import java.util.List;

/**
 * 分镜设计服务接口
 * 参考需求文档功能3：场景分析与分镜设计
 */
public interface StoryboardService {

    /**
     * 根据小说结构生成完整分镜脚本
     * @param structure 小说结构
     * @return 分镜列表
     */
    List<StoryboardPanel> generateStoryboard(NovelStructure structure);

    /**
     * 为单个场景设计分镜
     * @param scene 场景信息
     * @param index 分镜序号
     * @return 分镜面板
     */
    StoryboardPanel designPanel(SceneInfo scene, int index);

    /**
     * 验证分镜合理性
     * @param panels 分镜列表
     * @return 验证结果（true-通过，false-有警告）
     */
    boolean validateStoryboard(List<StoryboardPanel> panels);

    /**
     * 生成绘画Prompt
     * @param panel 分镜面板
     * @param style 漫画风格（japanese/chinese/realistic）
     * @return 英文Prompt
     */
    String generatePrompt(StoryboardPanel panel, String style);

    /**
     * 生成绘画Prompt（带角色一致性）
     * 核心方法！自动注入角色的详细描述，保证一致性
     * 
     * @param panel 分镜面板
     * @param style 漫画风格（japanese/chinese/realistic）
     * @param novelId 小说ID（用于检索角色描述）
     * @return 英文Prompt（包含角色一致性描述）
     */
    String generatePrompt(StoryboardPanel panel, String style, Long novelId);
}
