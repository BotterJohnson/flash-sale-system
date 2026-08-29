package com.botter.shop.canal.rabbitmq;

import com.botter.shop.common.rabbitmq.CanalMessage;
import com.botter.shop.common.utils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-28 18:51
 * @Description 描述信息
 */
@Service
public class MqSender {
    private static final Logger logger = LoggerFactory.getLogger(MqSender.class);
    
    @Autowired
    AmqpTemplate amqpTemplate;
    
    public void receiveMessage(String message) {
        logger.info("Direct message: {}", message);
        amqpTemplate.convertAndSend(MqcConfig.QUEUE, message);
    }
    

    public void receiveTopicQueue1(String message) {
        logger.info("Topic.queue1 message: {}", message);
        
    }
    
    public void receiveTopicQueue2(String message) {
        logger.info("Topic.queue2 message: {}", message);
    }
    

    public void receiveHeaderQueue(String message) {
        logger.info("Header queue: {}", message);
    }

    /**
     * 发送 canal 变更消息到 canal.queue，由 MysqlListener 编程式调用。
     * 注意：这是发送方法，绝不能加 @RabbitListener，否则本服务会自己消费掉消息，
     * 导致 search-service 收不到（且 CanalMessage 与 String 类型不匹配会转换失败丢消息）。
     */
    public void sendCanalMessage(CanalMessage cm) {
        String msg = BeanUtils.beanToString(cm);
        logger.info("send CanalMessage message:{}", msg);
        amqpTemplate.convertAndSend(MqcConfig.CANAL_QUEUE, msg);
    }
    
    public void sendTopicMessage(Object mes){
        String msg = BeanUtils.beanToString(mes);
        logger.info("send message:{}", msg);
        amqpTemplate.convertAndSend(MqcConfig.TOPIC_EXCHANGE , "topic.key1" , msg +"1");
        amqpTemplate.convertAndSend(MqcConfig.TOPIC_EXCHANGE, "topic.key2" , msg+"2");
    }
    
    public void sendHeaderMessage(Object mes){
        String msg = BeanUtils.beanToString(mes);
        logger.info("send message:{}", msg);
        amqpTemplate.convertAndSend(MqcConfig.HEADERS_EXCHANGE , "" , msg);
    }
    
    public void sendFanoutMessage(Object mes){
        String msg = BeanUtils.beanToString(mes);
        logger.info("send message:{}", msg);
        amqpTemplate.convertAndSend(MqcConfig.FANOUT_EXCHANGE , "", msg);
    }
    
    
}
