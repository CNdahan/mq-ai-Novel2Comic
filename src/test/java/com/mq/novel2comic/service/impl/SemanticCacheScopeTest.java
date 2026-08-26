package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.model.dto.ai.AigcConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SemanticCacheScopeTest {

    @Test
    void cacheScopeChangesWithProviderModelAndResolution() {
        AigcConfig oneK = AigcConfig.builder()
                .provider("grok")
                .model("grok-imagine-image")
                .resolution("1k")
                .build();
        AigcConfig twoK = AigcConfig.builder()
                .provider("grok")
                .model("grok-imagine-image")
                .resolution("2k")
                .build();

        assertEquals("grok:grok-imagine-image:1k", ImageCacheScope.from(oneK));
        assertEquals("grok:grok-imagine-image:2k", ImageCacheScope.from(twoK));
    }

    @Test
    void cacheScopeNormalizesValues() {
        AigcConfig config = AigcConfig.builder()
                .provider(" GROK ")
                .model(" Grok-Imagine-Image ")
                .resolution("1024x1024")
                .build();

        assertEquals("grok:grok-imagine-image:1k", ImageCacheScope.from(config));
    }
}
