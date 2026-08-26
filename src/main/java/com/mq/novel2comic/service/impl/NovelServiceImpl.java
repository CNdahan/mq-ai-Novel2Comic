package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.exception.ThrowUtils;
import com.mq.novel2comic.mapper.NovelMapper;
import com.mq.novel2comic.model.dto.novel.*;
import com.mq.novel2comic.model.entity.CharacterProfile;
import com.mq.novel2comic.model.entity.Novel;
import com.mq.novel2comic.service.CharacterExtractService;
import com.mq.novel2comic.service.NovelService;
import com.mq.novel2comic.service.UnifiedLLMClient;
import com.mq.novel2comic.utils.LlmSystemPrompts;
import com.mq.novel2comic.utils.PromptTemplateUtils;
import com.mq.novel2comic.utils.TextCleanUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author MQ
* @description 针对表【novel(小说表)】的数据库操作Service实现
* @createDate 2025-10-21 15:30:52
*/
@Slf4j
@Service
public class NovelServiceImpl extends ServiceImpl<NovelMapper, Novel>
    implements NovelService{

    static final int MIN_CONTENT_LENGTH = 300;
    static final int MAX_CONTENT_LENGTH = 10_000;

    @Resource
    private UnifiedLLMClient llmClient;

    @Resource
    private CharacterExtractService characterExtractService;

    @Resource
    private com.mq.novel2comic.service.CharacterProfileService characterProfileService;

    @Resource
    private com.mq.novel2comic.service.CharacterConsistencyService characterConsistencyService;

    @Override
    public NovelStructure parseNovel(String text) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(text), ErrorCode.PARAMS_ERROR, "小说内容不能为空");
        // 文本清洗
        String cleanedText = TextCleanUtils.clean(text);
        ThrowUtils.throwIf(cleanedText.isEmpty(), ErrorCode.PARAMS_ERROR, "小说内容为空");
        // 构建 Prompt
        String prompt = PromptTemplateUtils.buildParsePrompt(cleanedText);
        // 调用 LLM 分析（此处预留接口，待 LLM 集成后实现）
        String response = callLLM(prompt);
        // 解析 JSON 结果
        NovelStructure structure = parseStructure(response);
        log.info("小说解析完成，角色数: {}, 场景数: {}",
                structure.getCharacters() != null ? structure.getCharacters().size() : 0,
                structure.getScenes() != null ? structure.getScenes().size() : 0);
        return structure;
    }

    /**
     * 调用 LLM 解析小说
     */
    private String callLLM(String prompt) {
        try {
            String response = llmClient.chat(prompt, LlmSystemPrompts.NOVEL_PARSER);
            log.debug("LLM响应: {}", response);
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用LLM失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "小说解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析结构化数据
     */
    private NovelStructure parseStructure(String response) {
        try {
            String json = extractJson(response);
            return JSONUtil.toBean(json, NovelStructure.class);
        } catch (Exception e) {
            log.error("解析小说结构失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解析小说结构失败");
        }
    }

    /**
     * 提取 JSON 内容
     */
    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    @Override
    public NovelUploadResponse uploadNovel(NovelUploadRequest request, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(StrUtil.isBlank(request.getContent()), ErrorCode.PARAMS_ERROR, "小说内容不能为空");
        int contentLength = request.getContent().length();
        ThrowUtils.throwIf(contentLength < MIN_CONTENT_LENGTH, ErrorCode.PARAMS_ERROR,
                "小说内容不能少于" + MIN_CONTENT_LENGTH + "字符");
        ThrowUtils.throwIf(contentLength > MAX_CONTENT_LENGTH, ErrorCode.PARAMS_ERROR,
                "小说内容不能超过" + MAX_CONTENT_LENGTH + "字符");
        // 创建小说实体
        Novel novel = new Novel();
        novel.setUserId(userId);
        novel.setNovelTitle(StrUtil.isBlank(request.getTitle()) ? "未命名小说" : request.getTitle());
        novel.setNovelContent(request.getContent());
        novel.setContentLength(contentLength);
        novel.setSourceType(request.getSourceType());
        novel.setStatus("pending");
        // 保存到数据库
        boolean saved = this.save(novel);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "小说上传失败");
        log.info("小说上传成功，ID: {}, 用户ID: {}, 长度: {}", novel.getId(), userId, contentLength);
        // 自动提取并存储角色（核心新增功能！）
        List<CharacterProfile> characters = new ArrayList<>();
        int characterCount = 0;
        try {
            log.info("开始自动提取角色：novelId={}", novel.getId());
            characters = characterExtractService.extractAndStore(novel.getId(), request.getContent());
            characterCount = characters.size();
            
            // 更新小说状态为已处理
            novel.setStatus("completed");
            this.updateById(novel);
            log.info("角色提取完成：novelId={}, 提取到{}个角色", novel.getId(), characterCount);
        } catch (Exception e) {
            log.error("角色提取失败：novelId={}", novel.getId(), e);
            // 不影响主流程，小说已保存成功
            novel.setStatus("failed");
            novel.setErrorMessage("角色提取失败: " + e.getMessage());
            this.updateById(novel);
        }
        // 计算预估分镜数和时间
        int estimatedPanels = contentLength / 300 + 1;
        String estimatedTime = estimatedPanels <= 5 ? "1-2分钟" : 
                              estimatedPanels <= 10 ? "2-3分钟" : "3-5分钟";
        return NovelUploadResponse.builder()
                .novelId(novel.getId())
                .title(novel.getNovelTitle())
                .contentLength(contentLength)
                .estimatedPanels(estimatedPanels)
                .estimatedTime(estimatedTime)
                .characterCount(characterCount)  // 新增：返回提取到的角色数量
                .build();
    }

    @Override
    public boolean retryCharacterExtraction(Long novelId, Long userId) {
        ThrowUtils.throwIf(novelId == null || novelId <= 0, ErrorCode.PARAMS_ERROR, "小说ID无效");
        Novel novel = this.getById(novelId);
        ThrowUtils.throwIf(novel == null, ErrorCode.NOT_FOUND_ERROR, "小说不存在");
        ThrowUtils.throwIf(!userId.equals(novel.getUserId()), ErrorCode.NO_AUTH_ERROR, "无权操作该小说");
        if ("processing".equals(novel.getStatus()) || "pending".equals(novel.getStatus())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "角色提取处理中，请稍后重试");
        }

        // 清理上一次可能只写入了一部分的角色，避免重试后出现重复角色。
        characterProfileService.remove(new LambdaQueryWrapper<CharacterProfile>()
                .eq(CharacterProfile::getNovelId, novelId));
        characterConsistencyService.clearCache(novelId, null);
        novel.setStatus("processing");
        novel.setErrorMessage(null);
        this.updateById(novel);

        try {
            List<CharacterProfile> profiles = characterExtractService.extractAndStore(
                    novelId, novel.getNovelContent());
            novel.setStatus("completed");
            novel.setErrorMessage(null);
            this.updateById(novel);
            log.info("角色重新提取完成：novelId={}, 提取到{}个角色", novelId, profiles.size());
            return true;
        } catch (Exception e) {
            log.error("角色重新提取失败：novelId={}", novelId, e);
            novel.setStatus("failed");
            novel.setErrorMessage("角色提取失败: " + e.getMessage());
            this.updateById(novel);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, novel.getErrorMessage());
        }
    }

    @Override
    public NovelDetailResponse getNovelDetail(Long novelId, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(novelId == null || novelId <= 0, ErrorCode.PARAMS_ERROR, "小说ID无效");
        // 查询小说
        Novel novel = this.getById(novelId);
        ThrowUtils.throwIf(novel == null, ErrorCode.NOT_FOUND_ERROR, "小说不存在");
        ThrowUtils.throwIf(!novel.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权访问该小说");
        // 查询角色列表
        List<CharacterProfile> characterProfiles = characterProfileService.lambdaQuery()
                .eq(CharacterProfile::getNovelId, novelId)
                .eq(CharacterProfile::getIsDelete, 0)
                .orderByAsc(CharacterProfile::getCreateTime)
                .list();
        // 构建响应
        return NovelDetailResponse.builder()
                .novelId(novel.getId())
                .userId(novel.getUserId())
                .title(novel.getNovelTitle())
                .content(novel.getNovelContent())
                .contentLength(novel.getContentLength())
                .status(novel.getStatus())
                .createdAt(novel.getCreateTime())
                .characters(characterProfiles)  // 返回角色列表
                .build();
    }

    @Override
    public IPage<NovelListItem> getUserNovelList(NovelQueryRequest request, Long userId) {
        // 参数校验
        int current = request.getCurrent();
        int pageSize = request.getPageSize();
        ThrowUtils.throwIf(current <= 0, ErrorCode.PARAMS_ERROR, "页码必须大于0");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "每页数量必须在1-50之间");
        // 构建查询条件
        LambdaQueryWrapper<Novel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Novel::getUserId, userId);
        // 状态筛选
        String status = request.getStatus();
        if (!"all".equals(status)) {
            queryWrapper.eq(Novel::getStatus, status);
        }
        // 按创建时间降序
        queryWrapper.orderByDesc(Novel::getCreateTime);
        // 分页查询
        Page<Novel> page = new Page<>(current, pageSize);
        Page<Novel> novelPage = this.page(page, queryWrapper);
        // 转换为 DTO
        Page<NovelListItem> resultPage = new Page<>(novelPage.getCurrent(), novelPage.getSize(), novelPage.getTotal());
        List<NovelListItem> items = novelPage.getRecords().stream()
                .map(novel -> NovelListItem.builder()
                        .novelId(novel.getId())
                        .title(novel.getNovelTitle())
                        .contentLength(novel.getContentLength())
                        .status(novel.getStatus())
                        .createdAt(novel.getCreateTime())
                        .comicCount(0) // TODO: 后续从漫画表统计
                        .build())
                .toList();
        resultPage.setRecords(items);
        return resultPage;
    }
}

