package com.botter.shop.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeckillMqConfig {

    public static final String EXCHANGE = "seckill.exchange";
    public static final String ROUTING_KEY = "seckill.order.create";
    public static final String QUEUE = "seckill.order.queue";

    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(seckillExchange()).with(ROUTING_KEY);
    }
}
