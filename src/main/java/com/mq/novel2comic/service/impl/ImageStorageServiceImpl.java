package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.service.ImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.Base64;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图片存储服务实现
 * 将OSS临时URL的图片下载并保存到本地
 * 
 * @author MQ
 */
@Service
@Slf4j
public class ImageStorageServiceImpl implements ImageStorageService {
    
    @Resource
    private RestTemplate restTemplate;
    
    @Value("${image.storage.path:./images/}")
    private String storagePath;
    
    @Value("${server.port:8123}")
    private String serverPort;
    
    @PostConstruct
    public void init() {
        try {
            // 创建存储目录
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("✅ 图片存储目录创建成功: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("❌ 图片存储目录创建失败", e);
        }
    }
    
    @Override
    public String downloadAndSave(String ossUrl, Long comicId, int panelIndex) {
        try {
            if (ossUrl == null || ossUrl.isBlank()) {
                throw new RuntimeException("图片地址为空");
            }
            // 生成文件名
            String filename = resolveFilename(ossUrl, comicId, panelIndex);
            Path filePath = Paths.get(storagePath, filename);
            // 检查是否已经下载过
            if (Files.exists(filePath)) {
                log.info("✅ 图片已存在，跳过下载: {}", filename);
                return getLocalUrl(comicId, panelIndex);
            }
            log.info("📥 开始下载图片: {}", filename);
            log.debug("原始URL: {}", ossUrl);
            byte[] imageData;
            if (ossUrl.startsWith("data:image/")) {
                imageData = decodeDataUrl(ossUrl);
            } else {
                // 🔑 关键修复：使用URI对象避免RestTemplate再次编码
                // RestTemplate.getForObject(String) 会对URL进行编码，导致已编码的URL被双重编码
                // 使用URI对象可以告诉RestTemplate这是一个已经构建好的URI，不要再编码
                URI uri = URI.create(ossUrl);
                log.debug("使用URI下载: {}", uri);
                // 下载图片 - 使用URI对象而不是String
                imageData = restTemplate.getForObject(uri, byte[].class);
            }
            if (imageData == null || imageData.length == 0) {
                throw new RuntimeException("下载的图片数据为空");
            }
            // 保存到本地
            Files.write(filePath, imageData);
            log.info("✅ 图片保存成功: {}, 大小: {} bytes", filename, imageData.length);
            return getLocalUrl(comicId, panelIndex);
        } catch (Exception e) {
            log.error("❌ 图片下载保存失败: comicId={}, panelIndex={}", comicId, panelIndex, e);
            log.error("失败原因: {}", e.getMessage());
            // ⚠️ 重要：下载失败时返回null，而不是过期的OSS URL
            // 这样前端可以显示占位符，而不是显示会403的URL
            return null;
        }
    }
    
    @Override
    public boolean exists(Long comicId, int panelIndex) {
        return Files.exists(Paths.get(storagePath, findExistingFilename(comicId, panelIndex)));
    }

    @Override
    public byte[] readImage(Long comicId, int panelIndex) {
        try {
            Path filePath = Paths.get(storagePath, findExistingFilename(comicId, panelIndex));
            if (!Files.exists(filePath)) {
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取图片失败: comicId={}, panelIndex={}", comicId, panelIndex, e);
            return null;
        }
    }
    
    @Override
    public String getLocalUrl(Long comicId, int panelIndex) {
        String filename = findExistingFilename(comicId, panelIndex);
        return String.format("http://localhost:%s/api/static/images/%s", serverPort, filename);
    }
    
    @Override
    public boolean deleteImage(Long comicId, int panelIndex) {
        try {
            String[] extensions = {"png", "jpg", "jpeg", "webp"};
            boolean deleted = false;
            for (String extension : extensions) {
                String filename = String.format("comic_%d_panel_%d.%s", comicId, panelIndex, extension);
                Path filePath = Paths.get(storagePath, filename);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("✅ 图片文件删除成功: {}", filename);
                    deleted = true;
                    break;
                }
            }
            if (!deleted) {
                log.warn("⚠️ 图片文件不存在，跳过删除: comicId={}, panelIndex={}", comicId, panelIndex);
            }
            return deleted;
        } catch (IOException e) {
            log.error("❌ 图片文件删除失败: comicId={}, panelIndex={}", comicId, panelIndex, e);
            return false;
        }
    }
    
    @Override
    public int deleteAllImages(Long comicId, int panelCount) {
        int deletedCount = 0;
        log.info("🗑️ 开始删除漫画的所有图片: comicId={}, panelCount={}", comicId, panelCount);
        for (int i = 1; i <= panelCount; i++) {
            if (deleteImage(comicId, i)) {
                deletedCount++;
            }
        }
        log.info("✅ 图片删除完成: comicId={}, 成功删除 {}/{} 个文件", 
                comicId, deletedCount, panelCount);
        return deletedCount;
    }

    private String resolveFilename(String source, Long comicId, int panelIndex) {
        String extension = "jpg";
        if (source.startsWith("data:image/")) {
            int mimeStart = "data:image/".length();
            int mimeEnd = source.indexOf(';', mimeStart);
            if (mimeEnd > mimeStart) {
                String mime = source.substring(mimeStart, mimeEnd).toLowerCase();
                if (mime.contains("png")) {
                    extension = "png";
                } else if (mime.contains("webp")) {
                    extension = "webp";
                } else if (mime.contains("jpeg") || mime.contains("jpg")) {
                    extension = "jpg";
                }
            }
        }
        return String.format("comic_%d_panel_%d.%s", comicId, panelIndex, extension);
    }

    private String findExistingFilename(Long comicId, int panelIndex) {
        String[] extensions = {"png", "jpg", "jpeg", "webp"};
        for (String extension : extensions) {
            String filename = String.format("comic_%d_panel_%d.%s", comicId, panelIndex, extension);
            if (Files.exists(Paths.get(storagePath, filename))) {
                return filename;
            }
        }
        return String.format("comic_%d_panel_%d.jpg", comicId, panelIndex);
    }

    private byte[] decodeDataUrl(String dataUrl) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0 || commaIndex == dataUrl.length() - 1) {
            throw new IllegalArgumentException("无效的data URL");
        }
        String base64Part = dataUrl.substring(commaIndex + 1);
        return Base64.getDecoder().decode(base64Part);
    }
}

