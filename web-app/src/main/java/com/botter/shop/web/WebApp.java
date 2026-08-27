package com.botter.shop.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @ProjectName botter-shop-mic
 * @Author      Botter
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.botter.shop.common", "com.botter.shop.web"})
public class WebApp {
    public static void main(String[] args) {
        SpringApplication.run(WebApp.class, args);
    }
}
