package com.mq.novel2comic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.mq.novel2comic.mapper")
public class Novel2comicApplication {

    public static void main(String[] args) {
        SpringApplication.run(Novel2comicApplication.class, args);
    }

}
