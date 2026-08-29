package com.botter.shop.canal.rabbitmq;

import com.botter.shop.common.utils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-28 18:50
 * @Description 描述信息
 */
@Service
public class MqReceiver {
    private static Logger logger = LoggerFactory.getLogger(MqReceiver.class);

    @Autowired
    AmqpTemplate amqpTemplate;
    
    public void send(Object message){
        var beanToString = BeanUtils.beanToString(message);
        logger.info("Send message by Direct: {}", message);
        amqpTemplate.convertAndSend(MqcConfig.QUEUE ,  beanToString);
    }

    public void sendTopic(Object message) {
        String msg = BeanUtils.beanToString(message);
        logger.info("send topic message:" + msg);
        amqpTemplate.convertAndSend(MqcConfig.TOPIC_EXCHANGE, "topic.key1", msg + "1");
        amqpTemplate.convertAndSend(MqcConfig.TOPIC_EXCHANGE, "topic.key2", msg + "2");
    }


    public void sendFanout(Object message) {
        String msg = BeanUtils.beanToString(message);
        logger.info("send fanout message:" + msg);
        amqpTemplate.convertAndSend(MqcConfig.FANOUT_EXCHANGE, "", msg);
    }

    public void sendHeader(Object message) {
        String msg = BeanUtils.beanToString(message);
        logger.info("send fanout message:" + msg);
        MessageProperties properties = new MessageProperties();
        properties.setHeader("header1", "value1");
        properties.setHeader("header2", "value2");
        Message obj = new Message(msg.getBytes(), properties);
        amqpTemplate.convertAndSend(MqcConfig.HEADERS_EXCHANGE, "", obj);
    }
    
}
