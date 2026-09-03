/*
package com.botter.shop.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

*/
/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-31 12:25
 * @Description 描述信息
 *//*

public class AuthFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 目前统一鉴权由 SaReactorFilter 完成，这里直接放行；不能返回 null，否则过滤器链会中断
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
*/
