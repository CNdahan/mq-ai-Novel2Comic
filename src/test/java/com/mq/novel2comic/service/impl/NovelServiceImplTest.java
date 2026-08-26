package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.exception.BusinessException;
import com.mq.novel2comic.model.dto.novel.NovelUploadRequest;
import com.mq.novel2comic.model.entity.CharacterProfile;
import com.mq.novel2comic.model.entity.Novel;
import com.mq.novel2comic.service.CharacterExtractService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NovelServiceImplTest {

    @Test
    void uploadNovelAcceptsTenThousandCharacters() {
        NovelServiceImpl service = serviceWithSuccessfulStorage();

        assertDoesNotThrow(() -> service.uploadNovel(request("文".repeat(10_000)), 1L));
    }

    @Test
    void uploadNovelRejectsMoreThanTenThousandCharacters() {
        NovelServiceImpl service = new NovelServiceImpl();

        assertThrows(BusinessException.class,
                () -> service.uploadNovel(request("文".repeat(10_001)), 1L));
    }

    private NovelServiceImpl serviceWithSuccessfulStorage() {
        NovelServiceImpl service = new NovelServiceImpl() {
            @Override
            public boolean save(Novel novel) {
                novel.setId(1L);
                return true;
            }

            @Override
            public boolean updateById(Novel novel) {
                return true;
            }
        };
        CharacterExtractService characterExtractService = mock(CharacterExtractService.class);
        when(characterExtractService.extractAndStore(any(), anyString()))
                .thenReturn(List.<CharacterProfile>of());
        ReflectionTestUtils.setField(service, "characterExtractService", characterExtractService);
        return service;
    }

    private NovelUploadRequest request(String content) {
        NovelUploadRequest request = new NovelUploadRequest();
        request.setTitle("测试章节");
        request.setContent(content);
        request.setSourceType("direct");
        return request;
    }
}
