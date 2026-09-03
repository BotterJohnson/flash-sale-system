package com.botter.shop.order.vo;



import com.botter.shop.order.model.Order;
import com.botter.shop.order.model.OrderItem;

import java.util.List;

public class OrderVO extends Order {
    private List<OrderItem> orderItems;

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
}
