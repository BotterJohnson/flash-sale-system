package com.botter.shop.canal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.botter.shop.canal","com.botter.shop.common"})
@EnableFeignClients
public class CanalApp 
{
    public static void main( String[] args )
    {
        SpringApplication.run(CanalApp.class, args);
    }
}
