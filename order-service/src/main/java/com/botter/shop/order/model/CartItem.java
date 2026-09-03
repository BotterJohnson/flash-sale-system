package com.botter.shop.order.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "cart_item", schema = "order")
public class CartItem {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "create_time", nullable = false)
    private Instant createTime;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "price", nullable = false)
    private Long price;

    @ColumnDefault("'1'")
    @Column(name = "count", columnDefinition = "int UNSIGNED not null")
    private Long count;
    

}