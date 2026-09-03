package com.botter.shop.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-31 13:09
 * @Description 用户服务启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.botter.shop.common", "com.botter.shop.user"})
public class UserServerApp {
    public static void main(String[] args) {
        SpringApplication.run(UserServerApp.class, args);
    }
}
