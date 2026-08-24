package com.mq.novel2comic.exception;

import lombok.Getter;

/**
 * 自定义错误码
 *
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),
    
    // 10xxx 参数错误
    PARAMS_ERROR(10001, "参数错误"),
    TEXT_LENGTH_LIMIT(10002, "文本长度超限"),
    FILE_FORMAT_ERROR(10003, "文件格式不支持"),
    
    // 20xxx 认证/权限错误
    NOT_LOGIN_ERROR(20001, "用户未登录"),
    TOKEN_EXPIRED(20002, "Token失效"),
    QUOTA_INSUFFICIENT(20003, "配额不足"),
    USER_ALREADY_EXISTS(20004, "用户已存在"),
    USER_NOT_FOUND(20005, "用户不存在"),
    PASSWORD_ERROR(20006, "密码错误"),
    
    // 30xxx 业务错误
    GENERATION_FAILED(30001, "生成任务失败"),
    API_TIMEOUT(30002, "API调用超时"),
    
    // 40xxx 资源错误
    NOT_FOUND_ERROR(40001, "资源不存在"),
    NO_AUTH_ERROR(40002, "无权访问资源"),

    // 50xxx 系统错误
    SYSTEM_ERROR(50001, "系统异常");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
