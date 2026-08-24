package com.mq.novel2comic.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.novel.NovelStructure;
import com.mq.novel2comic.model.dto.novel.ValidationResult;
import com.mq.novel2comic.model.dto.storyboard.StoryboardGenerateResponse;
import com.mq.novel2comic.model.dto.storyboard.StoryboardResponse;
import com.mq.novel2comic.model.dto.storyboard.UpdateStoryboardRequest;
import com.mq.novel2comic.model.entity.Novel;
import com.mq.novel2comic.model.entity.StoryboardPanel;
import com.mq.novel2comic.model.enums.SceneTypeEnum;
import com.mq.novel2comic.model.enums.ShotTypeEnum;
import com.mq.novel2comic.service.NovelService;
import com.mq.novel2comic.service.StoryboardPanelService;
import com.mq.novel2comic.service.StoryboardService;
import com.mq.novel2comic.service.StoryboardValidator;
import com.mq.novel2comic.service.StoryboardVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分镜控制器
 * 参考需求文档功能3：场景分析与分镜设计
 * API设计参考文档05-API接口文档
 * 
 * @author MQ
 */
@RestController
@RequestMapping("/api/storyboard")
@Slf4j
public class StoryboardController {

    @Resource
    private StoryboardPanelService storyboardPanelService;

    @Resource
    private StoryboardValidator storyboardValidator;
    
    @Resource
    private StoryboardService storyboardService;
    
    @Resource
    private NovelService novelService;
    
    @Resource
    private StoryboardVersionService storyboardVersionService;

    /**
     * 为小说生成分镜（支持多版本）
     * 
     * API: POST /api/storyboard/generate/{novelId}
     * Query参数：
     *   - keepOld: 是否保留旧版本（true-保留，false-覆盖），默认true
     *   - setCurrent: 是否设置为当前版本，默认true
     *   - versionNote: 版本说明（可选）
     */
    @PostMapping("/generate/{novelId}")
    public BaseResponse<StoryboardGenerateResponse> generateStoryboard(
            @PathVariable Long novelId,
            @RequestParam(required = false, defaultValue = "true") Boolean keepOld,
            @RequestParam(required = false, defaultValue = "true") Boolean setCurrent,
            @RequestParam(required = false) String versionNote) {
        log.info("开始为小说生成分镜, novelId: {}, keepOld: {}, versionNote: {}", 
                novelId, keepOld, versionNote);
        // 1. 查询小说
        Novel novel = novelService.getById(novelId);
        if (novel == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "小说不存在");
        }
        // 2. 解析小说
        NovelStructure structure = novelService.parseNovel(novel.getNovelContent());
        // 3. 生成分镜脚本
        List<com.mq.novel2comic.model.dto.novel.StoryboardPanel> panels = 
                storyboardService.generateStoryboard(structure);
        if (panels == null || panels.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分镜生成失败，请重试");
        }
        // 4. 确定版本号
        Integer version;
        if (keepOld) {
            // 保留旧版本，生成新版本
            version = storyboardVersionService.getNextVersion(novelId);
            log.info("生成新版本, version: {}", version);
        } else {
            // 覆盖模式：删除所有旧版本，使用版本1
            QueryWrapper<StoryboardPanel> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("novelId", novelId);
            deleteWrapper.eq("isDelete", 0);
            long deletedCount = storyboardPanelService.count(deleteWrapper);
            if (deletedCount > 0) {
                storyboardPanelService.remove(deleteWrapper);
                log.info("覆盖模式：删除旧分镜数据, novelId: {}, 数量: {}", novelId, deletedCount);
            }
            version = 1;
        }
        // 5. 保存新分镜到数据库
        for (int i = 0; i < panels.size(); i++) {
            com.mq.novel2comic.model.dto.novel.StoryboardPanel panel = panels.get(i);
            StoryboardPanel entity = new StoryboardPanel();
            entity.setNovelId(novelId);
            entity.setVersion(version);
            entity.setIsCurrent(setCurrent ? 1 : 0);
            entity.setVersionNote(versionNote);
            entity.setPanelIndex(i + 1);
            entity.setSceneType(panel.getSceneType());
            entity.setShotType(panel.getShotType());
            entity.setDescriptionCn(panel.getDescriptionCn());
            entity.setDescriptionEn(panel.getPrompt());
            // 直接设置List对象，JacksonTypeHandler会自动处理JSON序列化
            entity.setCharacterList(panel.getCharacters());
            entity.setEnvironment(panel.getEnvironment());
            entity.setMood(panel.getMood());
            entity.setDialogueText(panel.getOriginalText());
            entity.setIsDelete(0);
            storyboardPanelService.save(entity);
        }
        // 6. 如果设置为当前版本，需要将其他版本标记为非当前
        if (setCurrent) {
            storyboardVersionService.setCurrentVersion(novelId, version);
        }
        log.info("分镜生成完成, novelId: {}, version: {}, 分镜数: {}", novelId, version, panels.size());
        // 7. 构建响应
        StoryboardGenerateResponse response = StoryboardGenerateResponse.builder()
                .novelId(novelId)
                .panelCount(panels.size())
                .status("completed")
                .message("分镜生成成功（版本 " + version + "）")
                .build();
        return ResultUtils.success(response);
    }
    
    /**
     * 获取小说的所有分镜（支持版本选择）
     * 
     * API: GET /api/storyboard/list/{novelId}
     * Query参数：
     *   - version: 版本号（可选，默认获取当前版本）
     */
    @GetMapping("/list/{novelId}")
    public BaseResponse<StoryboardResponse> getStoryboardsByNovelId(
            @PathVariable Long novelId,
            @RequestParam(required = false) Integer version) {
        log.info("获取小说分镜列表, novelId: {}, version: {}", novelId, version);
        // 如果未指定版本，获取当前版本
        if (version == null) {
            version = storyboardVersionService.getCurrentVersion(novelId);
            if (version == 0) {
                log.warn("小说 {} 没有分镜数据", novelId);
                return ResultUtils.success(StoryboardResponse.builder()
                        .novelId(novelId)
                        .panels(new ArrayList<>())
                        .totalCount(0)
                        .build());
            }
        }
        // 查询指定版本的分镜列表
        List<StoryboardPanel> storyboards = storyboardVersionService.getVersionPanels(novelId, version);
        if (storyboards.isEmpty()) {
            log.warn("小说 {} 的版本 {} 没有分镜数据", novelId, version);
            return ResultUtils.success(StoryboardResponse.builder()
                    .novelId(novelId)
                    .panels(new ArrayList<>())
                    .totalCount(0)
                    .build());
        }
        // 转换为VO
        List<StoryboardResponse.StoryboardPanelVO> panelVOs = storyboards.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        // 构建响应
        StoryboardResponse response = StoryboardResponse.builder()
                .novelId(novelId)
                .panels(panelVOs)
                .totalCount(panelVOs.size())
                .estimatedTime(calculateEstimatedTime(panelVOs.size()))
                .build();
        return ResultUtils.success(response);
    }

    /**
     * 根据ID获取分镜详情
     * 
     * API: GET /api/storyboard/{id}
     */
    @GetMapping("/{id}")
    public BaseResponse<StoryboardResponse.StoryboardPanelVO> getStoryboardById(@PathVariable Long id) {
        log.info("获取分镜详情, id: {}", id);
        StoryboardPanel storyboard = storyboardPanelService.getById(id);
        if (storyboard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分镜不存在");
        }
        StoryboardResponse.StoryboardPanelVO vo = convertToVO(storyboard);
        return ResultUtils.success(vo);
    }

    /**
     * 更新分镜
     * 
     * API: PUT /api/storyboard/{id}
     */
    @PutMapping("/{id}")
    public BaseResponse<StoryboardResponse.StoryboardPanelVO> updateStoryboard(
            @PathVariable Long id, 
            @RequestBody UpdateStoryboardRequest request) {
        log.info("更新分镜, id: {}, request: {}", id, request);
        // 查询原分镜
        StoryboardPanel storyboard = storyboardPanelService.getById(id);
        if (storyboard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分镜不存在");
        }
        // 更新字段
        if (request.getSceneType() != null) {
            storyboard.setSceneType(request.getSceneType());
        }
        if (request.getShotType() != null) {
            storyboard.setShotType(request.getShotType());
        }
        if (request.getDescriptionCn() != null) {
            storyboard.setDescriptionCn(request.getDescriptionCn());
        }
        if (request.getDescriptionEn() != null) {
            storyboard.setDescriptionEn(request.getDescriptionEn());
        }
        if (request.getCharacters() != null) {
            // 直接设置List对象，JacksonTypeHandler会自动处理JSON序列化
            storyboard.setCharacterList(request.getCharacters());
        }
        if (request.getEnvironment() != null) {
            storyboard.setEnvironment(request.getEnvironment());
        }
        if (request.getMood() != null) {
            storyboard.setMood(request.getMood());
        }
        if (request.getDialogueText() != null) {
            storyboard.setDialogueText(request.getDialogueText());
        }
        // 保存更新
        boolean result = storyboardPanelService.updateById(storyboard);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        // 返回更新后的数据
        StoryboardResponse.StoryboardPanelVO vo = convertToVO(storyboard);
        return ResultUtils.success(vo);
    }

    /**
     * 删除分镜
     * 
     * API: DELETE /api/storyboard/{id}
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteStoryboard(@PathVariable Long id) {
        log.info("删除分镜, id: {}", id);
        StoryboardPanel storyboard = storyboardPanelService.getById(id);
        if (storyboard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分镜不存在");
        }
        // 软删除
        storyboard.setIsDelete(1);
        boolean result = storyboardPanelService.updateById(storyboard);
        return ResultUtils.success(result);
    }

    /**
     * 验证分镜列表
     * 
     * API: POST /api/storyboard/validate/{novelId}
     */
    @PostMapping("/validate/{novelId}")
    public BaseResponse<ValidationResult> validateStoryboards(@PathVariable Long novelId) {
        log.info("验证分镜列表, novelId: {}", novelId);
        // 查询分镜列表
        QueryWrapper<StoryboardPanel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("novelId", novelId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByAsc("panelIndex");
        List<StoryboardPanel> storyboards = storyboardPanelService.list(queryWrapper);
        if (storyboards.isEmpty()) {
            return ResultUtils.success(ValidationResult.error("分镜列表为空"));
        }
        // 转换为DTO
        List<com.mq.novel2comic.model.dto.novel.StoryboardPanel> panels = storyboards.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        // 执行验证
        ValidationResult result = storyboardValidator.validate(panels);
        return ResultUtils.success(result);
    }

    /**
     * 转换为VO
     */
    private StoryboardResponse.StoryboardPanelVO convertToVO(StoryboardPanel entity) {
        List<String> characters = null;
        if (entity.getCharacterList() != null) {
            try {
                // JacksonTypeHandler 已经将 JSON 反序列化为 List 对象，直接类型转换
                if (entity.getCharacterList() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> characterList = (List<String>) entity.getCharacterList();
                    characters = characterList;
                } else {
                    // 降级处理：如果不是List，尝试解析
                    log.warn("characterList 不是 List 类型: {}", entity.getCharacterList().getClass());
                    characters = JSONUtil.toList(entity.getCharacterList().toString(), String.class);
                }
            } catch (Exception e) {
                log.warn("解析角色列表失败: {}", entity.getCharacterList(), e);
            }
        }
        return StoryboardResponse.StoryboardPanelVO.builder()
                .id(String.valueOf(entity.getId()))
                .index(entity.getPanelIndex())
                .sceneType(entity.getSceneType())
                .sceneTypeDesc(getSceneTypeDesc(entity.getSceneType()))
                .shotType(entity.getShotType())
                .shotTypeDesc(getShotTypeDesc(entity.getShotType()))
                .descriptionCn(entity.getDescriptionCn())
                .characters(characters)
                .environment(entity.getEnvironment())
                .mood(entity.getMood())
                .dialogueText(entity.getDialogueText())
                .build();
    }

    /**
     * 转换为DTO
     */
    private com.mq.novel2comic.model.dto.novel.StoryboardPanel convertToDTO(StoryboardPanel entity) {
        List<String> characters = null;
        if (entity.getCharacterList() != null) {
            try {
                // JacksonTypeHandler 已经将 JSON 反序列化为 List 对象，直接类型转换
                if (entity.getCharacterList() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> characterList = (List<String>) entity.getCharacterList();
                    characters = characterList;
                } else {
                    // 降级处理：如果不是List，尝试解析
                    log.warn("characterList 不是 List 类型: {}", entity.getCharacterList().getClass());
                    characters = JSONUtil.toList(entity.getCharacterList().toString(), String.class);
                }
            } catch (Exception e) {
                log.warn("解析角色列表失败: {}", entity.getCharacterList(), e);
            }
        }
        return com.mq.novel2comic.model.dto.novel.StoryboardPanel.builder()
                .id(String.valueOf(entity.getId()))
                .index(entity.getPanelIndex())
                .sceneType(entity.getSceneType())
                .shotType(entity.getShotType())
                .descriptionCn(entity.getDescriptionCn())
                .prompt(entity.getDescriptionEn()) // 使用prompt字段存储英文描述
                .characters(characters)
                .environment(entity.getEnvironment())
                .mood(entity.getMood())
                .originalText(entity.getDialogueText())
                .build();
    }

    /**
     * 获取场景类型描述
     */
    private String getSceneTypeDesc(String sceneType) {
        if (sceneType == null) {
            return null;
        }
        SceneTypeEnum typeEnum = SceneTypeEnum.getByCode(sceneType);
        return typeEnum != null ? typeEnum.getDesc() : sceneType;
    }

    /**
     * 获取镜头类型描述
     */
    private String getShotTypeDesc(String shotType) {
        if (shotType == null) {
            return null;
        }
        ShotTypeEnum typeEnum = ShotTypeEnum.getByCode(shotType);
        return typeEnum != null ? typeEnum.getDesc() : shotType;
    }

    /**
     * 计算预估时间
     */
    private String calculateEstimatedTime(int panelCount) {
        // 假设每个分镜生成需要15-20秒
        int seconds = panelCount * 18;
        int minutes = seconds / 60;
        if (minutes == 0) {
            return "< 1分钟";
        } else if (minutes <= 2) {
            return "2-3分钟";
        } else {
            return minutes + "-" + (minutes + 1) + "分钟";
        }
    }

    // ==================== 版本管理相关接口 ====================

    /**
     * 获取小说的所有分镜版本列表
     * 
     * API: GET /api/storyboard/versions/{novelId}
     */
    @GetMapping("/versions/{novelId}")
    public BaseResponse<List<VersionInfo>> getAllVersions(@PathVariable Long novelId) {
        log.info("获取小说的所有版本, novelId: {}", novelId);
        List<Integer> versions = storyboardVersionService.getAllVersions(novelId);
        Integer currentVersion = storyboardVersionService.getCurrentVersion(novelId);
        List<VersionInfo> versionInfos = versions.stream()
                .map(v -> {
                    // 获取该版本的第一个分镜，用于获取版本说明
                    List<StoryboardPanel> panels = storyboardVersionService.getVersionPanels(novelId, v);
                    String versionNote = panels.isEmpty() ? null : panels.get(0).getVersionNote();
                    
                    return VersionInfo.builder()
                            .version(v)
                            .isCurrent(v.equals(currentVersion))
                            .panelCount(panels.size())
                            .versionNote(versionNote)
                            .createTime(panels.isEmpty() ? null : panels.get(0).getCreateTime())
                            .build();
                })
                .collect(Collectors.toList());
        return ResultUtils.success(versionInfos);
    }

    /**
     * 设置当前使用的版本
     * 
     * API: POST /api/storyboard/versions/{novelId}/current/{version}
     */
    @PostMapping("/versions/{novelId}/current/{version}")
    public BaseResponse<Boolean> setCurrentVersion(
            @PathVariable Long novelId,
            @PathVariable Integer version) {
        log.info("设置当前版本, novelId: {}, version: {}", novelId, version);
        boolean result = storyboardVersionService.setCurrentVersion(novelId, version);
        return ResultUtils.success(result);
    }

    /**
     * 删除指定版本
     * 
     * API: DELETE /api/storyboard/versions/{novelId}/{version}
     */
    @DeleteMapping("/versions/{novelId}/{version}")
    public BaseResponse<Boolean> deleteVersion(
            @PathVariable Long novelId,
            @PathVariable Integer version) {
        log.info("删除版本, novelId: {}, version: {}", novelId, version);
        boolean result = storyboardVersionService.deleteVersion(novelId, version);
        return ResultUtils.success(result);
    }

    /**
     * 复制版本
     * 
     * API: POST /api/storyboard/versions/{novelId}/copy
     * Body: { "sourceVersion": 1, "targetVersion": 2 }
     */
    @PostMapping("/versions/{novelId}/copy")
    public BaseResponse<Boolean> copyVersion(
            @PathVariable Long novelId,
            @RequestBody CopyVersionRequest request) {
        log.info("复制版本, novelId: {}, from: {} to: {}", 
                novelId, request.getSourceVersion(), request.getTargetVersion());
        boolean result = storyboardVersionService.copyVersion(
                novelId, request.getSourceVersion(), request.getTargetVersion());
        return ResultUtils.success(result);
    }

    // ==================== 内部类 ====================

    /**
     * 版本信息VO
     */
    @lombok.Builder
    @lombok.Data
    public static class VersionInfo {
        private Integer version;
        private Boolean isCurrent;
        private Integer panelCount;
        private String versionNote;
        private Date createTime;
    }

    /**
     * 复制版本请求
     */
    @lombok.Data
    public static class CopyVersionRequest {
        private Integer sourceVersion;
        private Integer targetVersion;
    }
}

