package com.botter.shop.seckill.dto;

/**
 * 发布时写进 Redis 的活动快照，秒杀接口用它替代查库。
 * 时间统一转 epoch 毫秒，避免 JSON 反序列化 LocalDateTime 的时区坑。
 */
public record SeckillActivityCache(
        Long seckillGoodsId,   // 活动主键，订单表要用
        Long goodsId,
        String goodsName,
        Long seckillPrice,
        long startTime,        // epoch millis
        long endTime
) {
}
