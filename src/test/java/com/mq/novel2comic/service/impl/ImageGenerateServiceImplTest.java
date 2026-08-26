package com.mq.novel2comic.service.impl;

import com.mq.novel2comic.model.dto.image.ImageGenerateResult;
import com.mq.novel2comic.model.entity.StoryboardPanel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageGenerateServiceImplTest {

    @Test
    void generateBatchKeepsSuccessfulPanelsWhenOnePanelFails() {
        ImageGenerateServiceImpl service = new ImageGenerateServiceImpl() {
            @Override
            public CompletableFuture<ImageGenerateResult> generatePanelAsync(
                    String taskId, StoryboardPanel storyboard, String style) {
                if (storyboard.getPanelIndex() == 2) {
                    return CompletableFuture.failedFuture(new RuntimeException("upstream gateway error"));
                }
                return CompletableFuture.completedFuture(ImageGenerateResult.builder()
                        .storyboardId(storyboard.getId())
                        .panelIndex(storyboard.getPanelIndex())
                        .imageUrl("https://example.com/" + storyboard.getPanelIndex() + ".png")
                        .isCached(false)
                        .build());
            }
        };

        List<ImageGenerateResult> results = service.generateBatch(
                "task-1",
                List.of(panel(1L, 1), panel(2L, 2), panel(3L, 3)),
                "japanese"
        );

        assertEquals(List.of(1L, 3L), results.stream().map(ImageGenerateResult::getStoryboardId).toList());
    }

    @Test
    void generateBatchRetriesAFailedPanelAndRestoresTheFullPanelSet() {
        AtomicInteger panelTwoAttempts = new AtomicInteger();
        ImageGenerateServiceImpl service = new ImageGenerateServiceImpl() {
            @Override
            public CompletableFuture<ImageGenerateResult> generatePanelAsync(
                    String taskId, StoryboardPanel storyboard, String style) {
                if (storyboard.getPanelIndex() == 2
                        && panelTwoAttempts.incrementAndGet() == 1) {
                    return CompletableFuture.failedFuture(new RuntimeException("temporary upstream error"));
                }
                return CompletableFuture.completedFuture(ImageGenerateResult.builder()
                        .storyboardId(storyboard.getId())
                        .panelIndex(storyboard.getPanelIndex())
                        .imageUrl("https://example.com/" + storyboard.getPanelIndex() + ".png")
                        .isCached(false)
                        .build());
            }
        };

        List<ImageGenerateResult> results = service.generateBatch(
                "task-retry",
                List.of(panel(1L, 1), panel(2L, 2), panel(3L, 3)),
                "japanese"
        );

        assertEquals(List.of(1L, 2L, 3L), results.stream()
                .map(ImageGenerateResult::getStoryboardId).toList());
        assertEquals(2, panelTwoAttempts.get());
    }

    private StoryboardPanel panel(Long id, int panelIndex) {
        StoryboardPanel panel = new StoryboardPanel();
        panel.setId(id);
        panel.setPanelIndex(panelIndex);
        return panel;
    }
}
