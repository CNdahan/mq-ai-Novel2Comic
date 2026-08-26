package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.model.dto.ai.AigcConfig;

import java.util.Locale;

/**
 * Identifies the image-generation configuration used by a semantic cache entry.
 * Images from different models or resolutions must never share a cache result.
 */
final class ImageCacheScope {

    private ImageCacheScope() {
    }

    static String from(AigcConfig config) {
        if (config == null) {
            return "unknown:unknown:" + ImageSizePolicy.DEFAULT_RESOLUTION;
        }
        String provider = normalize(config.getProvider());
        String model = normalize(config.getModel());
        String resolution = ImageSizePolicy.normalizeResolution(config.getResolution());
        return provider + ":" + model + ":" + resolution;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
