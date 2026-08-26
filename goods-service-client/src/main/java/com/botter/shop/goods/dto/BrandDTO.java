package com.botter.shop.goods.dto;

import java.time.Instant;
import java.util.Date;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-24 01:08
 * @Description 描述信息
 */
public record BrandDTO(
        Long id,
        String name,
        Character beginLetter,
        Date createTime,
        Date updateTime
) {
}