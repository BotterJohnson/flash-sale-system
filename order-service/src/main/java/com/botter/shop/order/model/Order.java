package com.botter.shop.order.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "orders", schema = "order")
public class Order {
    @Id
    // 雪花ID超出JS Number安全范围(2^53)，序列化为字符串防止前端精度丢失
    @JsonSerialize(using = ToStringSerializer.class)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Size(max = 127)
    @NotNull
    @ColumnDefault("''")
    @Column(name = "receive_address", nullable = false, length = 127)
    private String receiveAddress;

    @ColumnDefault("'0'")
    @Column(name = "pay_type", columnDefinition = "tinyint UNSIGNED not null")
    private Short payType;

    @Size(max = 255)
    @NotNull
    @ColumnDefault("''")
    @Column(name = "consumer_msg", nullable = false)
    private String consumerMsg;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "discount_price", nullable = false)
    private Long discountPrice;

    @NotNull
    @ColumnDefault("'0'")
    @Column(name = "actual_price", nullable = false)
    private Long actualPrice;

    @ColumnDefault("'0'")
    @Column(name = "status", columnDefinition = "smallint UNSIGNED not null")
    private Integer status;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "create_time", nullable = false)
    private Instant createTime;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "update_time", nullable = false)
    private Instant updateTime;


}