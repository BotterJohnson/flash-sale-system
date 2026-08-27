package com.botter.shop.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.botter.shop.goods.api")
@ComponentScan(basePackages = {"com.botter.shop.search",
        "com.botter.shop.common",
        "com.botter.shop.goods"})
public class SearchApp
{
    public static void main( String[] args )
    {
        SpringApplication.run(SearchApp.class, args);
    }
}
