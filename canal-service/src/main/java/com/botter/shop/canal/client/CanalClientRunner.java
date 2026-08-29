package com.botter.shop.canal.client;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.botter.shop.canal.config.CanalClientProperties;
import com.botter.shop.canal.listener.MysqlListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 自研 Canal 客户端
 *
 * 说明：com.xpand starter-canal 只兼容 Spring Boot 2.x（javax + spring.factories），
 * 在 Spring Boot 3.x 下不会生效。这里直接基于官方 canal.client 编写客户端，
 * 采用「拉取 -> 处理 -> ack」的单线程循环模型，断线自动重连。
 *
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-29 02:36
 * @Description 描述信息
 */
@Component
@EnableConfigurationProperties(CanalClientProperties.class)
public class CanalClientRunner {

    private static final Logger log = LoggerFactory.getLogger(CanalClientRunner.class);

    /** 断线重连的等待毫秒数 */
    private static final long RECONNECT_INTERVAL_MS = 5000;

    private final CanalClientProperties properties;
    private final MysqlListener mysqlListener;

    private volatile boolean running = false;
    private volatile CanalConnector connector;
    private Thread workThread;

    public CanalClientRunner(CanalClientProperties properties, MysqlListener mysqlListener) {
        this.properties = properties;
        this.mysqlListener = mysqlListener;
    }

    @PostConstruct
    public void start() {
        running = true;
        workThread = new Thread(this::process, "canal-client-0");
        workThread.start();
        log.info("Canal 客户端启动，目标 {}:{} 实例 {}，订阅规则 [{}]",
                properties.host(), properties.port(), properties.destination(), properties.subscribeFilter());
    }

    /**
     * 主循环：连接 -> 持续拉取；连接异常时等待后重连
     */
    private void process() {
        while (running) {
            try {
                connect();
                while (running) {
                    // 1. 批量拉取，不自动 ack
                    Message message = connector.getWithoutAck(properties.batchSize());
                    long batchId = message.getId();
                    try {
                        // 2. 处理数据
                        if (batchId != -1 && !message.getEntries().isEmpty()) {
                            handle(message.getEntries());
                        }
                        // 3. 处理成功后提交 ack，告诉服务端这批可以丢弃
                        connector.ack(batchId);
                    } catch (Exception e) {
                        // 处理失败则回滚，服务端下次仍会下发这批数据
                        log.error("Canal 消息处理失败，已回滚批次 batchId={}", batchId, e);
                        connector.rollback(batchId);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    log.error("Canal 连接异常，{}ms 后重连", RECONNECT_INTERVAL_MS, e);
                    sleep(RECONNECT_INTERVAL_MS);
                }
            }
        }
    }

    /**
     * 建立连接并订阅，rollback() 让服务端从上次未 ack 的位置继续下发
     */
    private void connect() {
        connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(properties.host(), properties.port()),
                properties.destination(),
                properties.username(),
                properties.password());
        connector.connect();
        connector.subscribe(properties.subscribeFilter());
        connector.rollback();
        log.info("Canal 客户端已连接 {}:{}，destination={}，filter={}",
                properties.host(), properties.port(), properties.destination(), properties.subscribeFilter());
    }

    /**
     * 解析 binlog 条目并分发给监听器
     */
    private void handle(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            // 跳过事务开始/结束标记，它们不携带行数据
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN
                    || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }

            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                log.warn("解析 binlog 数据失败，table={}", entry.getHeader().getTableName(), e);
                continue;
            }

            String schema = entry.getHeader().getSchemaName();
            String table = entry.getHeader().getTableName();
            CanalEntry.EventType eventType = rowChange.getEventType();

            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                mysqlListener.onEvent(schema, table, eventType, rowData);
            }
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (workThread != null) {
            workThread.interrupt();
        }
        if (connector != null) {
            try {
                connector.disconnect();
            } catch (Exception ignored) {
            }
        }
        log.info("Canal 客户端已停止");
    }

    private void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
