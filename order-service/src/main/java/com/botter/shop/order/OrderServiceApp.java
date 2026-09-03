package com.botter.shop.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.botter.shop.common" , "com.botter.shop.order"})
@EnableFeignClients(basePackages = "com.botter.shop.goods.api")
@EnableTransactionManagement()
public class OrderServiceApp 
{
    public static void main( String[] args )
    {
        SpringApplication.run(OrderServiceApp.class, args);
    }
}
