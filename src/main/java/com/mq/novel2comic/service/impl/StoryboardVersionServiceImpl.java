package com.mq.novel2comic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.entity.StoryboardPanel;
import com.mq.novel2comic.service.StoryboardPanelService;
import com.mq.novel2comic.service.StoryboardVersionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分镜版本管理服务实现
 * 
 * @author MQ
 */
@Slf4j
@Service
public class StoryboardVersionServiceImpl implements StoryboardVersionService {

    @Resource
    private StoryboardPanelService storyboardPanelService;

    @Override
    public Integer getNextVersion(Long novelId) {
        QueryWrapper<StoryboardPanel> wrapper = new QueryWrapper<>();
        wrapper.eq("novelId", novelId);
        wrapper.eq("isDelete", 0);
        wrapper.select("MAX(version) as version");
        wrapper.last("LIMIT 1");
        StoryboardPanel panel = storyboardPanelService.getOne(wrapper);
        if (panel == null || panel.getVersion() == null) {
            return 1; // 第一个版本
        }
        return panel.getVersion() + 1;
    }

    @Override
    public Integer getCurrentVersion(Long novelId) {
        QueryWrapper<StoryboardPanel> wrapper = new QueryWrapper<>();
        wrapper.eq("novelId", novelId);
        wrapper.eq("isCurrent", 1);
        wrapper.eq("isDelete", 0);
        wrapper.select("version");
        wrapper.last("LIMIT 1");
        StoryboardPanel panel = storyboardPanelService.getOne(wrapper);
        return (panel != null && panel.getVersion() != null) ? panel.getVersion() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setCurrentVersion(Long novelId, Integer version) {
        log.info("设置当前版本, novelId: {}, version: {}", novelId, version);
        // 1. 将所有版本设置为非当前
        UpdateWrapper<StoryboardPanel> clearWrapper = new UpdateWrapper<>();
        clearWrapper.eq("novelId", novelId);
        clearWrapper.eq("isDelete", 0);
        clearWrapper.set("isCurrent", 0);
        storyboardPanelService.update(clearWrapper);
        // 2. 设置指定版本为当前
        UpdateWrapper<StoryboardPanel> setWrapper = new UpdateWrapper<>();
        setWrapper.eq("novelId", novelId);
        setWrapper.eq("version", version);
        setWrapper.eq("isDelete", 0);
        setWrapper.set("isCurrent", 1);
        boolean result = storyboardPanelService.update(setWrapper);
        log.info("设置当前版本完成, result: {}", result);
        return result;
    }

    @Override
    public List<Integer> getAllVersions(Long novelId) {
        QueryWrapper<StoryboardPanel> wrapper = new QueryWrapper<>();
        wrapper.eq("novelId", novelId);
        wrapper.eq("isDelete", 0);
        wrapper.select("DISTINCT version");
        wrapper.orderByDesc("version");
        return storyboardPanelService.list(wrapper).stream()
                .map(StoryboardPanel::getVersion)
                .filter(v -> v != null)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteVersion(Long novelId, Integer version) {
        log.info("删除版本, novelId: {}, version: {}", novelId, version);
        // 检查是否为当前版本
        Integer currentVersion = getCurrentVersion(novelId);
        if (currentVersion.equals(version)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能删除当前使用的版本");
        }
        // 软删除
        UpdateWrapper<StoryboardPanel> wrapper = new UpdateWrapper<>();
        wrapper.eq("novelId", novelId);
        wrapper.eq("version", version);
        wrapper.eq("isDelete", 0);
        wrapper.set("isDelete", 1);
        boolean result = storyboardPanelService.update(wrapper);
        log.info("删除版本完成, result: {}", result);
        return result;
    }

    @Override
    public List<StoryboardPanel> getVersionPanels(Long novelId, Integer version) {
        // 如果未指定版本，使用当前版本
        if (version == null) {
            version = getCurrentVersion(novelId);
            if (version == 0) {
                return new ArrayList<>();
            }
        }
        QueryWrapper<StoryboardPanel> wrapper = new QueryWrapper<>();
        wrapper.eq("novelId", novelId);
        wrapper.eq("version", version);
        wrapper.eq("isDelete", 0);
        wrapper.orderByAsc("panelIndex");
        return storyboardPanelService.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean copyVersion(Long novelId, Integer sourceVersion, Integer targetVersion) {
        log.info("复制版本, novelId: {}, from: {} to: {}", novelId, sourceVersion, targetVersion);
        // 1. 获取源版本的所有分镜
        List<StoryboardPanel> sourcePanels = getVersionPanels(novelId, sourceVersion);
        if (sourcePanels.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "源版本不存在");
        }
        // 2. 检查目标版本是否已存在
        long existCount = storyboardPanelService.count(
                new QueryWrapper<StoryboardPanel>()
                        .eq("novelId", novelId)
                        .eq("version", targetVersion)
                        .eq("isDelete", 0)
        );
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标版本已存在");
        }
        // 3. 复制分镜
        for (StoryboardPanel source : sourcePanels) {
            StoryboardPanel newPanel = new StoryboardPanel();
            newPanel.setNovelId(novelId);
            newPanel.setVersion(targetVersion);
            newPanel.setIsCurrent(0); // 新版本默认不是当前版本
            newPanel.setPanelIndex(source.getPanelIndex());
            newPanel.setSceneType(source.getSceneType());
            newPanel.setShotType(source.getShotType());
            newPanel.setDescriptionCn(source.getDescriptionCn());
            newPanel.setDescriptionEn(source.getDescriptionEn());
            newPanel.setCharacterList(source.getCharacterList());
            newPanel.setEnvironment(source.getEnvironment());
            newPanel.setMood(source.getMood());
            newPanel.setDialogueText(source.getDialogueText());
            newPanel.setIsDelete(0);
            storyboardPanelService.save(newPanel);
        }
        log.info("复制版本完成, 共复制{}个分镜", sourcePanels.size());
        return true;
    }
}

