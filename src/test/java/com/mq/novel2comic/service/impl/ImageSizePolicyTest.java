package com.mq.novel2comic.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageSizePolicyTest {

    @Test
    void defaultsToOneKAndMapsProviderSpecificSizes() {
        assertEquals("1k", ImageSizePolicy.normalizeResolution(null));
        assertEquals("1k", ImageSizePolicy.normalizeResolution("unknown"));
        assertEquals("1024x1024", ImageSizePolicy.sizeFor("1k"));
        assertEquals("1024*1024", ImageSizePolicy.wanxSizeFor("1k"));
        assertEquals("2k", ImageSizePolicy.normalizeResolution("2K"));
        assertEquals("2048x2048", ImageSizePolicy.sizeFor("2k"));
        assertEquals("2048*2048", ImageSizePolicy.wanxSizeFor("2k"));
        assertEquals("2k", ImageSizePolicy.grokResolution("2048x2048"));
    }
}
