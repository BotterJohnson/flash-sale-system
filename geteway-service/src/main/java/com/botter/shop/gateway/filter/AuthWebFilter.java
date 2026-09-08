package com.botter.shop.gateway.filter;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-09-01
 * @Description 网关统一鉴权 + token 透传。
 *              仅放行 /user/login、/user/register、/favicon.ico 与商品浏览，
 *              其余接口一律要求登录；鉴权失败返回 HTTP 401，避免前端误判为成功。
 */
@Component
public class AuthWebFilter implements WebFilter, Ordered {

    /** 原始路径（StripPrefix 之前），与网关路由的 Path 匹配 */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/login",
            "/user/register",
            "/favicon.ico",
            "/goods",
            // 秒杀列表/详情允许游客浏览，下单接口 /seckill/do 仍需登录
            "/seckill/list",
            "/seckill/detail"
    );

    /** 与 application.yml 中 globalcors.allowed-origins 保持一致 */
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            "http://localhost:18004",
            "http://127.0.0.1:18004"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 设置 sa-token 响应式上下文，否则 StpUtil 无法从请求头/Cookie 读取 token
        SaReactorSyncHolder.setContext(exchange);
        try {
            String method = exchange.getRequest().getMethod().name();
            // 放行跨域预检请求（CORS 由 globalcors 的 CorsWebFilter 处理）
            if ("OPTIONS".equalsIgnoreCase(method)) {
                return chain.filter(exchange);
            }

            String path = exchange.getRequest().getPath().value();
            if (isWhiteList(path)) {
                return chain.filter(exchange);
            }

            // 其余接口必须登录
            StpUtil.checkLogin();

            // 校验通过：把 token 透传给下游服务，保证 order-service 等能读到 satoken
            String token = readToken(exchange);
            ServerHttpRequest request = exchange.getRequest();
            if (token != null && !token.isBlank()) {
                request = request.mutate()
                        .header("satoken", token)
                        .build();
                exchange = exchange.mutate().request(request).build();
            }
            return chain.filter(exchange);
        } catch (cn.dev33.satoken.exception.SaTokenException e) {
            return unauthorized(exchange);
        } finally {
            SaReactorSyncHolder.clearContext();
        }
    }

    private boolean isWhiteList(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        for (String prefix : WHITE_LIST) {
            if (p.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 优先使用 sa-token 解析出的当前 token，其次从请求头 satoken / Cookie 兜底读取。
     */
    private String readToken(ServerWebExchange exchange) {
        try {
            String token = StpUtil.getTokenValue();
            if (token != null && !token.isBlank()) {
                return token;
            }
        } catch (Exception ignored) {
            // 忽略，走下方兜底
        }
        String header = exchange.getRequest().getHeaders().getFirst("satoken");
        if (header != null && !header.isBlank()) {
            return header;
        }
        String cookie = exchange.getRequest().getHeaders().getFirst("Cookie");
        if (cookie != null) {
            for (String part : cookie.split(";")) {
                String kv = part.trim();
                if (kv.startsWith("satoken=")) {
                    return kv.substring("satoken=".length());
                }
            }
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 浏览器跨域场景下，被拦截的错误响应也需携带 CORS 头，否则前端无法读取
        String origin = exchange.getRequest().getHeaders().getFirst("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.getHeaders().setAccessControlAllowOrigin(origin);
            response.getHeaders().setAccessControlAllowCredentials(true);
        }
        byte[] bytes = "{\"code\":500102,\"msg\":\"请先登录\",\"data\":null}"
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 早于 Spring Cloud Gateway 路由执行，晚于 CorsWebFilter
        return -100;
    }
}
