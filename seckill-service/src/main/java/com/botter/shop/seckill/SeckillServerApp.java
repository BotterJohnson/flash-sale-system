package com.botter.shop.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 秒杀服务启动类。
 *
 * <p>依赖：goods-service（发布时拉商品信息）；用户态由网关 sa-token 透传，秒杀服务用
 * StpUtil.getLoginIdAsLong() 直接拿到 userId，无需 Feign 调 user-service。
 *
 * @ProjectName botter-shop-mic
 * @Author Botter
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.botter.shop.common", "com.botter.shop.seckill"})
@EnableFeignClients(basePackages = {"com.botter.shop.goods.api"})
public class SeckillServerApp {
    public static void main(String[] args) {
        SpringApplication.run(SeckillServerApp.class, args);
    }
}
