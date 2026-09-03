package com.botter.shop.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 网关请求日志过滤器：记录每个请求的转发目标（路由 ID -> 目标服务）、响应状态码与耗时
 *
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-31 19:40
 * @Description 网关转发日志
 */
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    /** exchange attribute key：请求进入时间 */
    private static final String REQUEST_START_TIME = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String query = exchange.getRequest().getURI().getRawQuery();

        // 命中的路由（由 RoutePredicateHandlerMapping 在过滤器执行前写入）
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        String target = route == null
                ? "未匹配到路由"
                : route.getId() + " -> " + route.getUri();

        exchange.getAttributes().put(REQUEST_START_TIME, System.currentTimeMillis());
        log.info("[Gateway] 请求进入: {} {}{}  转发目标: {}", method, path,
                query == null ? "" : "?" + query, target);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long start = exchange.getAttribute(REQUEST_START_TIME);
            long cost = (start == null) ? -1 : System.currentTimeMillis() - start;
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            log.info("[Gateway] 请求完成: {} {} -> {}  状态: {}  耗时: {}ms",
                    method, path, target, status, cost);
        }));
    }

    /**
     * order 设为较小值，尽量在其他过滤器之前执行，保证日志能覆盖完整链路
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
