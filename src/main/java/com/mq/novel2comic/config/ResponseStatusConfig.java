package com.mq.novel2comic.config;

import com.mq.novel2comic.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * HTTP 状态码映射配置
 */
public class ResponseStatusConfig {

    /**
     * 根据业务错误码获取对应的 HTTP 状态码
     */
    public static HttpStatus getHttpStatus(int code) {
        if (code == 200) {
            return HttpStatus.OK;
        }
        // 10xxx 参数错误 -> 400
        if (code >= 10001 && code <= 10999) {
            return HttpStatus.BAD_REQUEST;
        }
        // 20xxx 认证/权限错误
        if (code >= 20001 && code <= 20999) {
            if (code == 20001 || code == 20002) {
                return HttpStatus.UNAUTHORIZED; // 401
            }
            return HttpStatus.FORBIDDEN; // 403
        }
        // 30xxx 业务错误 -> 500
        if (code >= 30001 && code <= 30999) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // 40xxx 资源错误 -> 404
        if (code >= 40001 && code <= 40999) {
            return HttpStatus.NOT_FOUND;
        }
        // 50xxx 系统错误 -> 500
        if (code >= 50001 && code <= 59999) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    
    /**
     * 根据 ErrorCode 获取对应的 HTTP 状态码
     */
    public static HttpStatus getHttpStatus(ErrorCode errorCode) {
        return getHttpStatus(errorCode.getCode());
    }
}
