package com.mq.novel2comic.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态文件访问控制器
 * 提供本地存储的图片访问
 * 
 * @author MQ
 */
@RestController
@RequestMapping("/static")
@Slf4j
public class StaticFileController {
    
    @Value("${image.storage.path:./images/}")
    private String storagePath;
    
    /**
     * 访问本地图片
     * GET /static/images/{filename}
     */
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(storagePath, filename);
            
            if (!Files.exists(filePath)) {
                log.warn("⚠️ 图片不存在: {}", filename);
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(filePath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl("public, max-age=31536000"); // 缓存1年
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            log.error("❌ 读取图片失败: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

