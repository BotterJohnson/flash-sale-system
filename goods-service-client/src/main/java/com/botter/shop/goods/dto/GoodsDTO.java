package com.botter.shop.goods.dto;

import java.time.Instant;
import java.util.Date;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-25 22:22
 * @Description 描述信息
 */
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record GoodsDTO(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String name,
        Long price,
        Long stock,
        String image,
        @JsonSerialize(using = ToStringSerializer.class) Long brandId,
        @JsonSerialize(using = ToStringSerializer.class) Long categoryId,
        String brandName,
        String categoryName,
        String paramJson,
        String specsJson,
        Date createTime,
        Date updateTime,
        Byte status) {
}
