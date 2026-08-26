package com.mq.novel2comic.service.impl;

/**
 * 统一处理图片分辨率配置，并转换为不同图片服务商的尺寸格式。
 */
final class ImageSizePolicy {

    static final String DEFAULT_RESOLUTION = "1k";
    private static final String ONE_K_SIZE = "1024x1024";
    private static final String TWO_K_SIZE = "2048x2048";
    private static final String ONE_K_WANX_SIZE = "1024*1024";
    private static final String TWO_K_WANX_SIZE = "2048*2048";

    private ImageSizePolicy() {
    }

    static String normalizeResolution(String resolution) {
        if (resolution == null || resolution.isBlank()) {
            return DEFAULT_RESOLUTION;
        }
        String normalized = resolution.trim().toLowerCase(java.util.Locale.ROOT);
        if ("2k".equals(normalized) || "2048".equals(normalized)
                || TWO_K_SIZE.equals(normalized)) {
            return "2k";
        }
        return DEFAULT_RESOLUTION;
    }

    static String sizeFor(String resolution) {
        return "2k".equals(normalizeResolution(resolution)) ? TWO_K_SIZE : ONE_K_SIZE;
    }

    static String wanxSizeFor(String resolution) {
        return "2k".equals(normalizeResolution(resolution)) ? TWO_K_WANX_SIZE : ONE_K_WANX_SIZE;
    }

    static String grokResolution(String resolution) {
        return normalizeResolution(resolution);
    }
}
