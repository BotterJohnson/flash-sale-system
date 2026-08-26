package com.botter.shop.goods.dto;

import java.time.Instant;
import java.util.Date;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-25 22:22
 * @Description 描述信息
 */
public record GoodsDTO(
        Long id,
        String name,
        Long price,
        Long stock,
        String image,
        Long brandId,
        Long categoryId,
        String brandName,
        String categoryName,
        String paramJson,
        String specsJson,
        Date createTime,
        Date updateTime,
        Byte status) {
}
