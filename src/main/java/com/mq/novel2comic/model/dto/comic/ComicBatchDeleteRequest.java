package com.mq.novel2comic.model.dto.comic;

import lombok.Data;
import java.util.List;

/** 批量删除漫画请求。 */
@Data
public class ComicBatchDeleteRequest {
    private List<Long> comicIds;
}
