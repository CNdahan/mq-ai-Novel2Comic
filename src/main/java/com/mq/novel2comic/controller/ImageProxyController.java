package com.mq.novel2comic.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;

/**
 * 图片代理控制器
 * 用于解决OSS图片的403 Forbidden问题
 * 
 * @author MQ
 */
@RestController
@RequestMapping("/image")
@Slf4j
public class ImageProxyController {

    @Resource
    private RestTemplate restTemplate;

    /**
     * 代理获取OSS图片
     * GET /image/proxy?url={imageUrl}
     */
    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        log.info("代理获取图片: {}", url.substring(0, Math.min(100, url.length())));
        try {
            // 使用RestTemplate下载图片
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                byte[].class
            );
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setCacheControl("public, max-age=31536000"); // 缓存1年
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Methods", "GET");
            log.info("✅ 图片代理成功，大小: {} bytes", response.getBody() != null ? response.getBody().length : 0);
            return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error("❌ 图片代理失败: {}", errorMsg);
            // 检查是否是URL过期
            if (errorMsg != null && errorMsg.contains("Request has expired")) {
                log.error("⏰ 图片URL已过期！建议重新生成漫画获取新的URL");
                errorMsg = "图片URL已过期，请重新生成漫画";
            } else if (errorMsg != null && errorMsg.contains("403")) {
                log.error("🔐 图片访问被拒绝（403），可能是OSS权限问题");
                errorMsg = "图片访问被拒绝（403 Forbidden）";
            }
            // 返回友好的错误信息
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Access-Control-Allow-Origin", "*");
            String jsonError = String.format(
                "{\"error\": true, \"message\": \"%s\", \"suggestion\": \"请重新生成漫画获取新的图片URL\"}", 
                errorMsg
            );
            return new ResponseEntity<>(
                jsonError.getBytes(),
                headers,
                HttpStatus.OK // 返回200但包含错误信息，避免浏览器报错
            );
        }
    }
}

