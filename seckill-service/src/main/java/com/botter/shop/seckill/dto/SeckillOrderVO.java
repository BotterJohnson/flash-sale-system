package com.botter.shop.seckill.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 秒杀订单视图对象。
 */
public record SeckillOrderVO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String orderNo,
        @JsonSerialize(using = ToStringSerializer.class) Long seckillGoodsId,
        @JsonSerialize(using = ToStringSerializer.class) Long goodsId,
        String goodsName,
        Long payPrice,
        Long createTime) {
}
