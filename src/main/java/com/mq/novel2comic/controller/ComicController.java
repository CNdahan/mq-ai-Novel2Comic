package com.mq.novel2comic.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.comic.ComicDetailResponse;
import com.mq.novel2comic.model.dto.comic.ComicBatchDeleteRequest;
import com.mq.novel2comic.model.dto.comic.ComicGenerateRequest;
import com.mq.novel2comic.model.dto.comic.ComicGenerateResponse;
import com.mq.novel2comic.model.dto.comic.ComicListItemResponse;
import com.mq.novel2comic.model.dto.comic.ComicQueryRequest;
import com.mq.novel2comic.model.entity.Comic;
import com.mq.novel2comic.model.entity.ComicPanel;
import com.mq.novel2comic.model.entity.Novel;
import com.mq.novel2comic.model.entity.StoryboardPanel;
import com.mq.novel2comic.service.ComicPanelService;
import com.mq.novel2comic.service.ComicService;
import com.mq.novel2comic.service.ImageStorageService;
import com.mq.novel2comic.service.ImageGenerateService;
import com.mq.novel2comic.service.NovelService;
import com.mq.novel2comic.service.StoryboardPanelService;
import com.mq.novel2comic.service.PromptBuilderService;
import com.mq.novel2comic.service.UserService;
import com.mq.novel2comic.service.impl.ComicGenerateServiceImpl;
import com.mq.novel2comic.utils.JwtUtils;
import com.mq.novel2comic.utils.SubtitleTextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.ExecutionException;

/**
 * 漫画控制器
 * @author MQ
 */
@RestController
@RequestMapping("/comic")
@Slf4j
public class ComicController {

    @Resource
    private ComicService comicService;
    
    @Resource
    private ComicPanelService comicPanelService;
    
    @Resource
    private ComicGenerateServiceImpl comicGenerateService;
    
    @Resource
    private JwtUtils jwtUtils;
    
    @Resource
    private ImageStorageService imageStorageService;
    
    @Resource
    private UserService userService;

    @Resource
    private StoryboardPanelService storyboardPanelService;

    @Resource
    private NovelService novelService;

    @Resource
    private ImageGenerateService imageGenerateService;

    @Resource
    private PromptBuilderService promptBuilderService;

    /**
     * 生成漫画（核心接口）
     * POST /comic/generate
     */
    @PostMapping("/generate")
    public BaseResponse<ComicGenerateResponse> generateComic(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ComicGenerateRequest request) {
        log.info("收到漫画生成请求: {}", request);
        // 1. 从Token中提取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        // 2. 检查用户剩余次数
        if (!userService.hasQuota(userId)) {
            Integer remainQuota = userService.getQuotaRemain(userId);
            log.warn("⚠️ 用户次数不足: userId={}, quotaRemain={}", userId, remainQuota);
            throw new BusinessException(ErrorCode.QUOTA_INSUFFICIENT, 
                    "生成次数不足，当前剩余: " + remainQuota + " 次");
        }
        // 3. 扣减用户次数
        boolean deducted = userService.deductQuota(userId, 1);
        if (!deducted) {
            log.error("❌ 次数扣减失败: userId={}", userId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "次数扣减失败，请稍后重试");
        }
        // 4. 调用生成服务
        ComicGenerateResponse response = comicGenerateService.generateComic(request, userId);
        // 5. 在响应中包含剩余次数
        Integer remainQuota = userService.getQuotaRemain(userId);
        log.info("✅ 漫画生成完成，剩余次数: {}", remainQuota);
        return ResultUtils.success(response, "漫画生成成功，剩余次数: " + remainQuota);
    }
    
    /**
     * 获取漫画生成结果（包含所有面板）
     * GET /comic/result/{comicId}
     */
    @GetMapping("/result/{comicId}")
    public BaseResponse<ComicDetailResponse> getComicResult(
            @PathVariable Long comicId) {
        log.info("获取漫画结果: comicId={}", comicId);
        // 查询漫画主记录
        Comic comic = comicService.getById(comicId);
        if (comic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "漫画不存在");
        }
        // 查询已经成功生成的面板
        QueryWrapper<ComicPanel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("comicId", comicId);  // 使用驼峰命名
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByAsc("panelIndex");
        List<ComicPanel> panels = comicPanelService.list(queryWrapper);

        // 失败的分镜不会有 comic_panel 记录，补回占位面板，允许用户直接重新生成。
        List<StoryboardPanel> storyboards = storyboardPanelService.lambdaQuery()
                .eq(StoryboardPanel::getNovelId, comic.getNovelId())
                .eq(StoryboardPanel::getIsCurrent, 1)
                .eq(StoryboardPanel::getIsDelete, 0)
                .orderByAsc(StoryboardPanel::getPanelIndex)
                .list();
        Map<Integer, ComicPanel> panelsByIndex = new LinkedHashMap<>();
        for (ComicPanel panel : panels) {
            panelsByIndex.put(panel.getPanelIndex(), panel);
        }
        Map<Integer, String> subtitlesByIndex = resolveSubtitles(comic, panels, storyboards);
        List<ComicPanel> displayPanels = new java.util.ArrayList<>();
        for (StoryboardPanel storyboard : storyboards) {
            ComicPanel panel = panelsByIndex.get(storyboard.getPanelIndex());
            if (panel == null) {
                panel = new ComicPanel();
                panel.setComicId(comicId);
                panel.setNovelId(comic.getNovelId());
                panel.setStoryboardId(storyboard.getId());
                panel.setPanelIndex(storyboard.getPanelIndex());
                panel.setStyle(comic.getStyle());
                panel.setPromptText(promptBuilderService.buildFinalPrompt(storyboard, comic.getStyle()));
                panel.setNegativePrompt(promptBuilderService.buildNegativePrompt());
                panel.setIsDelete(0);
            }
            panel.setSubtitleText(subtitlesByIndex.getOrDefault(panel.getPanelIndex(), ""));
            displayPanels.add(panel);
        }
        if (!storyboards.isEmpty()) {
            panels = displayPanels;
            if (!Objects.equals(comic.getPanelCount(), storyboards.size())) {
                comic.setPanelCount(storyboards.size());
                comicService.updateById(comic);
            }
        } else {
            // 历史作品可能没有当前分镜版本，仍使用面板保存的 storyboardId 显示对白。
            for (ComicPanel panel : panels) {
                panel.setSubtitleText(subtitlesByIndex.getOrDefault(panel.getPanelIndex(), ""));
            }
        }
        // 构建响应
        ComicDetailResponse response = ComicDetailResponse.builder()
                .comicId(comic.getId())
                .title(comic.getComicTitle())
                .style(comic.getStyle())
                .status(comic.getStatus())
                .panelCount(comic.getPanelCount())
                .panels(panels)
                .build();
        return ResultUtils.success(response);
    }

    private Map<Long, StoryboardPanel> loadStoryboardsById(List<ComicPanel> panels) {
        Set<Long> storyboardIds = new HashSet<>();
        for (ComicPanel panel : panels) {
            if (panel.getStoryboardId() != null) {
                storyboardIds.add(panel.getStoryboardId());
            }
        }
        Map<Long, StoryboardPanel> result = new LinkedHashMap<>();
        if (!storyboardIds.isEmpty()) {
            for (StoryboardPanel storyboard : storyboardPanelService.listByIds(storyboardIds)) {
                result.put(storyboard.getId(), storyboard);
            }
        }
        return result;
    }

    private Map<Integer, String> resolveSubtitles(
            Comic comic, List<ComicPanel> savedPanels, List<StoryboardPanel> currentStoryboards) {
        Map<Integer, StoryboardPanel> sourcesByIndex = new LinkedHashMap<>();
        for (StoryboardPanel storyboard : currentStoryboards) {
            sourcesByIndex.put(storyboard.getPanelIndex(), storyboard);
        }
        Map<Long, StoryboardPanel> storyboardsById = loadStoryboardsById(savedPanels);
        for (ComicPanel panel : savedPanels) {
            StoryboardPanel savedStoryboard = storyboardsById.get(panel.getStoryboardId());
            if (savedStoryboard != null) {
                sourcesByIndex.put(panel.getPanelIndex(), savedStoryboard);
            }
        }
        List<SubtitleTextUtils.PanelSource> sources = sourcesByIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new SubtitleTextUtils.PanelSource(
                        entry.getKey(), entry.getValue().getDialogueText(), entry.getValue().getSceneType()))
                .toList();
        Novel novel = novelService.getById(comic.getNovelId());
        String novelText = novel == null ? "" : novel.getNovelContent();
        return SubtitleTextUtils.assign(novelText, sources);
    }

    /**
     * 获取用户漫画列表（分页）
     * GET /comic/list
     */
    @GetMapping("/list")
    public BaseResponse<IPage<ComicListItemResponse>> getUserComicList(
            @RequestHeader("Authorization") String authHeader,
            ComicQueryRequest request) {
        log.info("获取用户漫画列表: request={}", request);
        // 从Token获取用户ID
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        IPage<ComicListItemResponse> response = comicService.getUserComicList(request, userId);
        return ResultUtils.success(response);
    }
    
    /**
     * 根据novelId获取最新的漫画
     * GET /comic/latest/{novelId}
     */
    @GetMapping("/latest/{novelId}")
    public BaseResponse<Comic> getLatestComicByNovelId(@PathVariable Long novelId) {
        log.info("获取小说最新漫画: novelId={}", novelId);
        // 查询该小说最新的漫画（按创建时间降序）
        QueryWrapper<Comic> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("novelId", novelId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");
        queryWrapper.last("LIMIT 1");
        Comic latestComic = comicService.getOne(queryWrapper);
        if (latestComic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该小说还没有生成漫画");
        }
        return ResultUtils.success(latestComic);
    }
    
    /**
     * 根据ID获取漫画详情
     */
    @GetMapping("/{id}")
    public BaseResponse<Comic> getComicById(@PathVariable Long id) {
        Comic comic = comicService.getById(id);
        return ResultUtils.success(comic);
    }

    /**
     * 删除漫画（包括本地图片文件）
     * DELETE /comic/{id}
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteComic(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        log.info("开始删除漫画: comicId={}", id);
        Long userId = getUserId(authHeader);
        return ResultUtils.success(deleteComicForUser(id, userId), "作品删除成功");
    }

    /** 重新生成漫画中的单个分镜图片。 */
    @PostMapping("/{comicId}/panel/{panelIndex}/regenerate")
    public BaseResponse<ComicPanel> regeneratePanel(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long comicId,
            @PathVariable Integer panelIndex) {
        Long userId = getUserId(authHeader);
        Comic comic = comicService.getById(comicId);
        if (comic == null || !userId.equals(comic.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该作品");
        }
        ComicPanel panel = comicPanelService.lambdaQuery()
                .eq(ComicPanel::getComicId, comicId)
                .eq(ComicPanel::getPanelIndex, panelIndex)
                .eq(ComicPanel::getIsDelete, 0)
                .one();
        StoryboardPanel storyboard = panel == null
                ? storyboardPanelService.lambdaQuery()
                    .eq(StoryboardPanel::getNovelId, comic.getNovelId())
                    .eq(StoryboardPanel::getPanelIndex, panelIndex)
                    .eq(StoryboardPanel::getIsCurrent, 1)
                    .eq(StoryboardPanel::getIsDelete, 0)
                    .one()
                : storyboardPanelService.getById(panel.getStoryboardId());
        if (storyboard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "对应分镜脚本不存在");
        }
        // 补全历史数据中缺失的 comic_panel 记录，避免生成成功后写入结果时出现空指针。
        if (panel == null) {
            panel = new ComicPanel();
            panel.setComicId(comicId);
            panel.setNovelId(comic.getNovelId());
            panel.setStoryboardId(storyboard.getId());
            panel.setPanelIndex(panelIndex);
            panel.setStyle(comic.getStyle());
            panel.setIsDelete(0);
        }
        try {
            String taskId = "panel-regenerate-" + comicId + "-" + panelIndex;
            com.mq.novel2comic.model.dto.image.ImageGenerateResult result =
                    imageGenerateService.generatePanelAsync(taskId, storyboard, comic.getStyle(), true).get();
            // downloadAndSave intentionally skips existing files; remove the old image first.
            imageStorageService.deleteImage(comicId, panelIndex);
            String localUrl = imageStorageService.downloadAndSave(result.getImageUrl(), comicId, panelIndex);
            panel.setImageUrl(localUrl == null ? result.getImageUrl() : localUrl);
            panel.setPromptText(result.getPrompt());
            panel.setGenerateTimeMs(result.getGenerateTimeMs());
            panel.setIsCached(result.getIsCached() ? 1 : 0);
            boolean saved = panel.getId() == null
                    ? comicPanelService.save(panel)
                    : comicPanelService.updateById(panel);
            if (!saved) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存新图片失败");
            }
            int storyboardCount = storyboardPanelService.lambdaQuery()
                    .eq(StoryboardPanel::getNovelId, comic.getNovelId())
                    .eq(StoryboardPanel::getIsCurrent, 1)
                    .eq(StoryboardPanel::getIsDelete, 0)
                    .count().intValue();
            if (storyboardCount > 0 && !Objects.equals(comic.getPanelCount(), storyboardCount)) {
                comic.setPanelCount(storyboardCount);
                comicService.updateById(comic);
            }
            return ResultUtils.success(panel, "分镜图片重新生成成功");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "重新生成被中断");
        } catch (ExecutionException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分镜图片生成失败: " + e.getCause().getMessage());
        }
    }

    /** 下载已生成的分镜原图压缩包。 */
    @GetMapping("/{comicId}/download")
    public ResponseEntity<ByteArrayResource> downloadPanels(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long comicId,
            @RequestParam(defaultValue = "true") boolean subtitles) {
        Comic comic = getOwnedComic(comicId, getUserId(authHeader));
        List<StoredPanel> storedPanels = readStoredPanels(comic, subtitles);
        if (storedPanels.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "当前作品没有可下载的图片");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                for (StoredPanel panel : storedPanels) {
                    zip.putNextEntry(new ZipEntry(String.format("panel_%02d.%s",
                            panel.index(), detectExtension(panel.bytes()))));
                    zip.write(panel.bytes());
                    zip.closeEntry();
                }
            }
            return fileResponse(output.toByteArray(), MediaType.parseMediaType("application/zip"),
                    "comic-" + comicId + "-panels.zip");
        } catch (IOException e) {
            log.error("批量下载漫画图片失败: comicId={}", comicId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量下载失败: " + e.getMessage());
        }
    }

    /** 按指定列数编排已生成的分镜，并返回一张PNG长图。 */
    @GetMapping("/{comicId}/layout")
    public ResponseEntity<ByteArrayResource> layoutPanels(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long comicId,
            @RequestParam(defaultValue = "1") Integer columns,
            @RequestParam(defaultValue = "true") boolean subtitles) {
        Comic comic = getOwnedComic(comicId, getUserId(authHeader));
        if (columns == null || columns < 1 || columns > 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编排列数只能是1到3列");
        }
        List<StoredPanel> storedPanels = readStoredPanels(comic, subtitles);
        if (storedPanels.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "当前作品没有可编排的图片");
        }
        try {
            byte[] layout = createLayout(storedPanels, columns);
            return fileResponse(layout, MediaType.IMAGE_PNG,
                    "comic-" + comicId + "-layout-" + columns + "col.png");
        } catch (IOException e) {
            log.error("批量编排漫画图片失败: comicId={}, columns={}", comicId, columns, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片编排失败: " + e.getMessage());
        }
    }

    private Comic getOwnedComic(Long comicId, Long userId) {
        Comic comic = comicService.getById(comicId);
        if (comic == null || !Objects.equals(comic.getIsDelete(), 0)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "漫画不存在");
        }
        if (!Objects.equals(comic.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该作品");
        }
        return comic;
    }

    private List<StoredPanel> readStoredPanels(Comic comic, boolean subtitles) {
        int panelCount = comic.getPanelCount() == null ? 0 : comic.getPanelCount();
        List<StoredPanel> panels = new ArrayList<>();
        Map<Integer, String> subtitlesByIndex = new LinkedHashMap<>();
        if (subtitles) {
            List<ComicPanel> savedPanels = comicPanelService.lambdaQuery()
                    .eq(ComicPanel::getComicId, comic.getId())
                    .eq(ComicPanel::getIsDelete, 0)
                    .list();
            List<StoryboardPanel> storyboards = storyboardPanelService.lambdaQuery()
                    .eq(StoryboardPanel::getNovelId, comic.getNovelId())
                    .eq(StoryboardPanel::getIsCurrent, 1)
                    .eq(StoryboardPanel::getIsDelete, 0)
                    .list();
            subtitlesByIndex.putAll(resolveSubtitles(comic, savedPanels, storyboards));
        }
        for (int index = 1; index <= panelCount; index++) {
            byte[] bytes = imageStorageService.readImage(comic.getId(), index);
            if (bytes != null && bytes.length > 0) {
                String subtitle = subtitlesByIndex.getOrDefault(index, "");
                byte[] exportBytes = bytes;
                if (!subtitle.isBlank()) {
                    try {
                        exportBytes = renderSubtitle(bytes, subtitle);
                    } catch (IOException e) {
                        log.warn("分镜{}字幕渲染失败，回退原图", index, e);
                    }
                }
                panels.add(new StoredPanel(index, exportBytes));
            }
        }
        return panels;
    }

    private byte[] renderSubtitle(byte[] bytes, String subtitle) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null) {
            return bytes;
        }
        int padding = Math.max(18, source.getWidth() / 32);
        int fontSize = Math.max(24, Math.min(56, source.getWidth() / 24));
        Font font = new Font("Microsoft YaHei", Font.PLAIN, fontSize);
        Graphics2D measureGraphics = source.createGraphics();
        measureGraphics.setFont(font);
        FontMetrics metrics = measureGraphics.getFontMetrics();
        List<String> lines = wrapSubtitle(subtitle, metrics, source.getWidth() - padding * 2);
        int lineHeight = metrics.getHeight();
        int captionHeight = lines.size() * lineHeight + padding * 2;
        measureGraphics.dispose();

        // 字幕栏位于图片外侧，原画区域保持完整，不再被黑框覆盖。
        BufferedImage output = new BufferedImage(source.getWidth(),
                source.getHeight() + captionHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, null);

        graphics.setFont(font);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, source.getHeight(), source.getWidth(), captionHeight);
        graphics.setColor(Color.WHITE);
        int textY = source.getHeight() + padding + metrics.getAscent();
        for (String line : lines) {
            int textX = (source.getWidth() - metrics.stringWidth(line)) / 2;
            graphics.drawString(line, textX, textY);
            textY += lineHeight;
        }
        graphics.dispose();
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ImageIO.write(output, "png", outputBytes);
        return outputBytes.toByteArray();
    }

    private List<String> wrapSubtitle(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\R")) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                String candidate = line + String.valueOf(paragraph.charAt(i));
                if (line.length() > 0 && metrics.stringWidth(candidate) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(paragraph.charAt(i));
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            }
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private byte[] createLayout(List<StoredPanel> panels, int columns) throws IOException {
        final int gap = 24;
        final int cellWidth = 1024;
        List<BufferedImage> images = new ArrayList<>();
        for (StoredPanel panel : panels) {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(panel.bytes()));
            if (source == null) {
                continue;
            }
            int height = Math.max(1, (int) Math.round(source.getHeight() * (cellWidth / (double) source.getWidth())));
            BufferedImage scaled = new BufferedImage(cellWidth, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, cellWidth, height, null);
            graphics.dispose();
            images.add(scaled);
        }
        if (images.isEmpty()) {
            throw new IOException("没有可识别的图片");
        }
        int rows = (images.size() + columns - 1) / columns;
        int[] rowHeights = new int[rows];
        for (int i = 0; i < images.size(); i++) {
            rowHeights[i / columns] = Math.max(rowHeights[i / columns], images.get(i).getHeight());
        }
        int outputWidth = columns * cellWidth + (columns - 1) * gap;
        int outputHeight = gap * (rows - 1);
        for (int height : rowHeights) {
            outputHeight += height;
        }
        BufferedImage canvas = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, outputWidth, outputHeight);
        int y = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int imageIndex = row * columns + col;
                if (imageIndex >= images.size()) {
                    break;
                }
                BufferedImage image = images.get(imageIndex);
                int x = col * (cellWidth + gap) + (cellWidth - image.getWidth()) / 2;
                graphics.drawImage(image, x, y, null);
            }
            y += rowHeights[row] + gap;
        }
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(canvas, "png", output);
        return output.toByteArray();
    }

    private ResponseEntity<ByteArrayResource> fileResponse(byte[] bytes, MediaType mediaType, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(bytes.length);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(bytes));
    }

    private String detectExtension(byte[] bytes) {
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "png";
        }
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return "jpg";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "webp";
        }
        return "img";
    }

    private record StoredPanel(int index, byte[] bytes) {
    }

    /** 批量删除漫画（包括本地图片文件）。 */
    @DeleteMapping("/batch")
    public BaseResponse<Integer> batchDeleteComics(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ComicBatchDeleteRequest request) {
        Long userId = getUserId(authHeader);
        List<Long> comicIds = request == null ? null : request.getComicIds();
        if (comicIds == null || comicIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请至少选择一个作品");
        }
        List<Long> distinctIds = comicIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择有效的作品");
        }
        List<Comic> comics = comicService.listByIds(distinctIds);
        if (comics.size() != distinctIds.size()
                || comics.stream().anyMatch(comic -> !Objects.equals(comic.getUserId(), userId)
                || !Objects.equals(comic.getIsDelete(), 0))) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "存在不存在或无权删除的作品");
        }
        int deletedCount = 0;
        for (Long comicId : distinctIds) {
            if (deleteComicForUser(comicId, userId)) {
                deletedCount++;
            }
        }
        return ResultUtils.success(deletedCount, "已删除 " + deletedCount + " 个作品");
    }

    private Long getUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        return userId;
    }

    private boolean deleteComicForUser(Long id, Long userId) {
        Comic comic = comicService.getById(id);
        if (comic == null || !Objects.equals(comic.getIsDelete(), 0)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "漫画不存在");
        }
        if (!Objects.equals(comic.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该作品");
        }
        int panelCount = comic.getPanelCount() == null ? 0 : comic.getPanelCount();
        int deletedImageCount = imageStorageService.deleteAllImages(id, panelCount);
        log.info("本地图片清理完成: comicId={}, 删除了 {}/{} 个文件", id, deletedImageCount, panelCount);
        UpdateWrapper<Comic> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                     .eq("userId", userId)
                     .eq("isDelete", 0)
                     .set("isDelete", 1);
        boolean result = comicService.update(updateWrapper);
        if (result) {
            log.info("✅ 漫画删除成功: comicId={}, title={}", id, comic.getComicTitle());
        } else {
            log.error("❌ 漫画删除失败: comicId={}", id);
        }
        return result;
    }
}
