package com.botter.shop.seckill.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 秒杀商品视图对象，字段名与前端 seckill.js 一一对应。
 *
 * <p>价格单位统一为"分"，时间统一用毫秒时间戳。
 */
public record SeckillGoodsVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        @JsonSerialize(using = ToStringSerializer.class) Long goodsId,
        String goodsName,
        String image,
        Long originalPrice,
        Long seckillPrice,
        Integer stock,
        Integer total,
        /** 0-未开始 1-进行中 2-已结束 */
        Integer status,
        Long startTime,
        Long endTime,
        /** 当前用户是否已抢到 */
        boolean bought,
        /** 已抢到时的订单号 */
        String orderId,
        /** 服务器当前时间，前端用它校正本地时钟 */
        Long serverTime) {
}
