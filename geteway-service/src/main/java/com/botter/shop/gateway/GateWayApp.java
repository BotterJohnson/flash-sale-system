package com.botter.shop.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import reactor.core.publisher.Mono;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class GateWayApp 
{
    public static void main( String[] args )
    {
        SpringApplication.run(GateWayApp.class, args);
    }
    
    @Bean(name = "ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange ->  Mono.just(exchange.getRequest().getRemoteAddress().getHostString());
    }
}
