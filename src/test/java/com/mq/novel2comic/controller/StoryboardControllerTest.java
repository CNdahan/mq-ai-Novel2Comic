package com.mq.novel2comic.controller;

import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.model.dto.storyboard.StoryboardGenerateResponse;
import com.mq.novel2comic.model.entity.GenerateTask;
import com.mq.novel2comic.model.entity.Novel;
import com.mq.novel2comic.service.GenerateTaskService;
import com.mq.novel2comic.service.NovelService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoryboardControllerTest {

    @Test
    void generateStoryboardReturnsTaskBeforeRunningLlmWork() {
        NovelService novelService = mock(NovelService.class);
        GenerateTaskService generateTaskService = mock(GenerateTaskService.class);
        AtomicReference<Runnable> submittedTask = new AtomicReference<>();

        Novel novel = new Novel();
        novel.setId(46L);
        novel.setUserId(1L);
        novel.setNovelContent("第二章 转机");
        when(novelService.getById(46L)).thenReturn(novel);
        when(generateTaskService.save(any(GenerateTask.class))).thenReturn(true);

        StoryboardController controller = new StoryboardController();
        ReflectionTestUtils.setField(controller, "novelService", novelService);
        ReflectionTestUtils.setField(controller, "generateTaskService", generateTaskService);
        ReflectionTestUtils.setField(controller, "comicTaskExecutor",
                (java.util.concurrent.Executor) submittedTask::set);

        BaseResponse<StoryboardGenerateResponse> response = assertTimeout(
                Duration.ofSeconds(1),
                () -> controller.generateStoryboard(46L, true, true, null));

        assertEquals(200, response.getCode());
        assertEquals("processing", response.getData().getStatus());
        assertFalse(response.getData().getTaskId().isBlank());
        assertNotNull(submittedTask.get());
        verify(generateTaskService).save(any(GenerateTask.class));
        verify(novelService, never()).parseNovel(anyString());
    }
}
