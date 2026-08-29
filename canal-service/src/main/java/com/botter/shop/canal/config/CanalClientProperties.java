package com.botter.shop.canal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Canal 客户端连接配置
 *
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-29 02:36
 * @Description 对应 application.yml 中 canal.client 前缀的配置
 */
@ConfigurationProperties(prefix = "canal.client")
public record CanalClientProperties(
        /** canal 服务端地址 */
        String host,
        /** canal 服务端端口（deployer 默认 11111） */
        int port,
        /** 实例名，必须与 canal 服务端 conf/ 目录下的实例目录名一致 */
        String destination,
        /** 服务端开启了客户端鉴权时才需要填（canal.properties 中的 canal.user/canal.pass） */
        String username,
        String password,
        /** 订阅过滤规则，正则表达式，如 goods\..* 表示监听 goods 库所有表 */
        String subscribeFilter,
        /** 每次批量拉取的最大条数 */
        int batchSize) {

    public CanalClientProperties {
        // 提供默认值，避免 yml 中漏配导致 NPE
        if (host == null || host.isBlank()) {
            host = "127.0.0.1";
        }
        if (port <= 0) {
            port = 11111;
        }
        if (destination == null || destination.isBlank()) {
            destination = "example";
        }
        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
        if (subscribeFilter == null || subscribeFilter.isBlank()) {
            subscribeFilter = ".*\\..*";
        }
        if (batchSize <= 0) {
            batchSize = 100;
        }
    }
}
