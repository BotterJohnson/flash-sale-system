package com.botter.shop.canal.listener;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.botter.shop.canal.rabbitmq.MqSender;
import com.botter.shop.common.rabbitmq.CanalMessage;
import com.botter.shop.common.rabbitmq.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MySQL 表变更监听器
 *
 * 说明：不再使用 com.xpand starter-canal 的 @CanalEventListener/@ListenPoint 注解
 * （该 starter 不兼容 Spring Boot 3.x）。改为由 CanalClientRunner 解析 binlog 后
 * 回调 onEvent()，通过 schema/table/eventType 自行过滤。
 *
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-29 02:09
 * @Description 描述信息
 */
@Component
public class MysqlListener {
    private static Logger log = LoggerFactory.getLogger(MysqlListener.class);

    @Autowired
    private MqSender mqSender;
    
    /**
     * 所有库表变更都会进入此方法，按需过滤
     *
     * @param schema    库名
     * @param table     表名
     * @param eventType 事件类型（INSERT/UPDATE/DELETE）
     * @param rowData   行数据（变更前/变更后的列值）
     */
    public void onEvent(String schema, String table, CanalEntry.EventType eventType, CanalEntry.RowData rowData) {
        // 只处理 goods 库的 goods 表
        if (!"goods".equals(schema) || !"goods".equals(table)) {
            return;
        }

        if (eventType == CanalEntry.EventType.INSERT || eventType == CanalEntry.EventType.UPDATE) {
            var afterColumnsList = rowData.getAfterColumnsList();
            for (CanalEntry.Column column : afterColumnsList) {
                if (column.getName().equals("id")) {
                    log.info("[{}] goods 表数据变更, id is {}", eventType, column.getValue());
                    if (eventType == CanalEntry.EventType.INSERT) {
                        mqSender.sendCanalMessage(new CanalMessage("goods" , "goods" , Long.valueOf(column.getValue()) , OperationType.ADD));
                    }else {
                        mqSender.sendCanalMessage(new CanalMessage("goods" , "goods" , Long.valueOf(column.getValue()) , OperationType.UPDATE));
                    }
                    break;
                }
            }
        } else if (eventType == CanalEntry.EventType.DELETE) {
            var beforeColumnsList = rowData.getBeforeColumnsList();
            for (CanalEntry.Column column : beforeColumnsList) {
                if (column.getName().equals("id")) {
                    log.info("[DELETE] goods 表数据删除, id is {}", column.getValue());
                    mqSender.sendCanalMessage(new CanalMessage("goods" , "goods" , Long.valueOf(column.getValue()) , OperationType.DELETE));
                    break;
                }
            }
        }
    }
}
