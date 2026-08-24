package com.mq.novel2comic.exception;

import com.mq.novel2comic.common.BaseResponse;
import com.mq.novel2comic.common.ResultUtils;
import com.mq.novel2comic.config.ResponseStatusConfig;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 */
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        BaseResponse<?> response = ResultUtils.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(ResponseStatusConfig.getHttpStatus(e.getCode())).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<?>> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        BaseResponse<?> response = ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
        return ResponseEntity.status(ResponseStatusConfig.getHttpStatus(ErrorCode.SYSTEM_ERROR)).body(response);
    }
}
