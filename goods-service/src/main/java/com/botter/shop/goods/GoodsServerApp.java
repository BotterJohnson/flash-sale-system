package com.botter.shop.goods;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 19:33
 * @Description 描述信息
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.botter.shop.common" , "com.botter.shop.goods"})
public class GoodsServerApp {
    public static void main(String[] args) {
        SpringApplication.run(GoodsServerApp.class, args);
    }
}
