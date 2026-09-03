package com.botter.shop.order.vo;

import java.io.Serializable;

/**
 * 购物车数量统计。
 * kinds：购物车中不同商品的种类数（与 cart_list 表格的行数对应）
 * total：购物车中所有商品的总件数（每行 count 之和）
 */
public class CartSizeVO implements Serializable {
    private long kinds;
    private long total;

    public CartSizeVO() {
    }

    public CartSizeVO(long kinds, long total) {
        this.kinds = kinds;
        this.total = total;
    }

    public long getKinds() {
        return kinds;
    }

    public void setKinds(long kinds) {
        this.kinds = kinds;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
