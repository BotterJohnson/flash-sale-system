package com.botter.shop.seckill.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

/**
 * 秒杀活动商品。
 *
 * <p>这里做了"字段冗余"：商品名、图片、原价直接从 goods-service 拷贝一份，
 * 秒杀列表页无需再回查商品服务，减少一次跨服务调用。
 */
@Getter
@Setter
@Entity
@Table(name = "seckill_goods", schema = "seckill")
public class SeckillGoods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** 商品服务中的商品 id */
    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    @Column(name = "goods_name", nullable = false, length = 127)
    private String goodsName;

    @ColumnDefault("''")
    @Column(name = "goods_img", nullable = false, length = 255)
    private String goodsImg;

    /** 商品原价（单位：分），发布活动时从 goods-service 拷贝 */
    @ColumnDefault("'0'")
    @Column(name = "origin_price", nullable = false)
    private Long originPrice;

    /** 秒杀价（单位：分） */
    @ColumnDefault("'0'")
    @Column(name = "seckill_price", nullable = false)
    private Long seckillPrice;

    /** 原始库存（发布时设定的活动总库存，不随抢购减少，用于计算"已抢 = total - stock"） */
    @ColumnDefault("'0'")
    @Column(name = "total", nullable = false)
    private Integer total;

    /** 当前剩余库存（抢购时递减） */
    @ColumnDefault("'0'")
    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
