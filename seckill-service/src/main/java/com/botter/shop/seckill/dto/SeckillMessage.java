package com.botter.shop.seckill.dto;

/**
 * 秒杀下单消息。orderNo 在生产端预生成，作为消费端的幂等键：
 * 同一条消息被重复消费时，靠订单表 order_no 唯一索引兜底，不会重复插单。
 */
public record SeckillMessage(String orderNo, Long goodsId, Long userId) {
}
