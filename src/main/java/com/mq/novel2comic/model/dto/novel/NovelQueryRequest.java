package com.mq.novel2comic.model.dto.novel;

import com.mq.novel2comic.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小说查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NovelQueryRequest extends PageRequest {

    /**
     * 状态筛选：all-全部, pending-待处理, processing-处理中, completed-已完成, failed-失败
     */
    private String status = "all";
}
