package com.botter.shop.seckill.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 秒杀订单。
 *
 * <p>唯一索引 (user_id, seckill_goods_id) 是"一人一单"的最后一道防线：
 * 即便 Redis 防重失效（如 Redis 重启），数据库也能拦住重复下单。
 */
@Getter
@Setter
@Entity
@Table(
        name = "seckill_order",
        schema = "seckill",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_goods", columnNames = {"user_id", "seckill_goods_id"})
)
public class SeckillOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 32)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 下单时的昵称，从 user-service 查询后冗余，避免列表页再查用户服务 */
    @Column(name = "nickname", length = 45)
    private String nickname;

    @Column(name = "seckill_goods_id", nullable = false)
    private Long seckillGoodsId;

    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    @Column(name = "goods_name", length = 127)
    private String goodsName;

    /** 实付金额（单位：分） */
    @Column(name = "pay_price", nullable = false)
    private Long payPrice;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
}
