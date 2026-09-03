package com.botter.shop.order.vo;



import com.botter.shop.order.model.CartItem;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;

public class CartItemVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private long itemId;
    // 前端加减数量需要用它调 /cart/incr /cart/decr
    @JsonSerialize(using = ToStringSerializer.class)
    private long goodsId;
    private String image;
    private String title;
    private long priceOrigin;
    private long priceNow;
    private Long count;
    private long totalPriceOrigin;
    private long totalPriceNow;

    public CartItemVO() {
    }

    public CartItemVO(CartItem cartItem) {
        this.itemId = cartItem.getId();
        this.goodsId = cartItem.getGoodsId();
        this.count = cartItem.getCount();
        this.priceOrigin = cartItem.getPrice();
        this.totalPriceOrigin = cartItem.getPrice() * cartItem.getCount();
    }

    public long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(long goodsId) {
        this.goodsId = goodsId;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public long getPriceNow() {
        return priceNow;
    }

    public void setPriceNow(long priceNow) {
        this.priceNow = priceNow;
    }

    public long getTotalPriceNow() {
        return totalPriceNow;
    }

    public void setTotalPriceNow(long totalPriceNow) {
        this.totalPriceNow = totalPriceNow;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getPriceOrigin() {
        return priceOrigin;
    }

    public void setPriceOrigin(long priceOrigin) {
        this.priceOrigin = priceOrigin;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getTotalPriceOrigin() {
        return totalPriceOrigin;
    }

    public void setTotalPriceOrigin(long totalPriceOrigin) {
        this.totalPriceOrigin = totalPriceOrigin;
    }
}
