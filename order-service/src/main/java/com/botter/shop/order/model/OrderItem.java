package com.botter.shop.order.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "order_item", schema = "order")
public class OrderItem {
    @Id
    // 雪花ID超出JS Number安全范围(2^53)，序列化为字符串防止前端精度丢失
    @JsonSerialize(using = ToStringSerializer.class)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "create_time", nullable = false)
    @CreationTimestamp
    private Instant createTime;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "update_time", nullable = false)
    @UpdateTimestamp
    private Instant updateTime;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "cart_item_id", nullable = false)
    private Long cartItemId;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "goods_id", nullable = false)
    private Long goodsId;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "order_id", nullable = false)
    // 雪花ID超出JS Number安全范围(2^53)，序列化为字符串防止前端精度丢失
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "price", nullable = false)
    private Long price;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "discount_price", nullable = false)
    private Long discountPrice;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "actual_price", nullable = false)
    private Long actualPrice;

    @ColumnDefault("'0'")
    @Column(name = "count", columnDefinition = "int UNSIGNED not null")
    private Long count;

    @Size(max = 255)
    @NotNull
    @ColumnDefault("''")
    @Column(name = "goods_name", nullable = false)
    private String goodsName;

    @Size(max = 511)
    @NotNull
    @ColumnDefault("''")
    @Column(name = "goods_image", nullable = false, length = 511)
    private String goodsImage;


}