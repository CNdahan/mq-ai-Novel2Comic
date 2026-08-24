package com.mq.novel2comic.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 限流时间窗口（秒）
     */
    int seconds() default 60;
    
    /**
     * 时间窗口内最大请求次数
     */
    int maxCount() default 100;
}
