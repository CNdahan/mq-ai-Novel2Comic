package com.mq.novel2comic.model.dto.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 通用图片生成响应
 */
@Data
public class ImageGenerationResponse {

    private List<Item> data;

    @Data
    public static class Item {
        private String url;

        @JsonProperty("b64_json")
        private String b64Json;
    }
}
