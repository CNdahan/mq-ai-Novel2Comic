package com.mq.novel2comic.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.exception.ErrorCode;
import com.mq.novel2comic.model.dto.ai.AigcConfig;
import com.mq.novel2comic.service.AigcModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AIGC模型发现服务实现
 */
@Slf4j
@Service
public class AigcModelServiceImpl implements AigcModelService {

    @Resource
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public List<String> listModels(AigcConfig config) {
        if (config == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AIGC配置不能为空");
        }
        if (StrUtil.isBlank(config.getApiKey())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "API Key不能为空");
        }
        String modelsUrl = resolveModelsUrl(config.getBaseUrl());
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(modelsUrl))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("模型列表调用失败: statusCode={}, body={}", response.statusCode(), response.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "获取模型列表失败: HTTP " + response.statusCode());
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取模型列表失败：返回为空");
            }
            if (body.trim().startsWith("<")) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取模型列表失败：接口返回了HTML页面，请检查 Base URL");
            }

            JsonNode root = objectMapper.readTree(body);
            Set<String> modelIds = new LinkedHashSet<>();
            JsonNode dataNode = root.get("data");
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    String id = textValue(item, "id");
                    if (StrUtil.isNotBlank(id)) {
                        modelIds.add(id);
                    }
                }
            }
            JsonNode modelsNode = root.get("models");
            if (modelsNode != null && modelsNode.isArray()) {
                for (JsonNode item : modelsNode) {
                    String id = textValue(item, "id");
                    if (StrUtil.isNotBlank(id)) {
                        modelIds.add(id);
                    }
                }
            }
            if (modelIds.isEmpty()) {
                String single = textValue(root, "id");
                if (StrUtil.isNotBlank(single)) {
                    modelIds.add(single);
                }
            }
            if (modelIds.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取模型列表失败：未识别到模型数据");
            }
            return new ArrayList<>(modelIds);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取模型列表异常: baseUrl={}", modelsUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取模型列表失败: " + e.getMessage());
        }
    }

    private String resolveModelsUrl(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Base URL不能为空");
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/images/generations")) {
            return normalized.substring(0, normalized.length() - "/images/generations".length()) + "/models";
        }
        if (normalized.endsWith("/models")) {
            return normalized;
        }
        return normalized + "/models";
    }

    private String textValue(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText(null);
    }
}
