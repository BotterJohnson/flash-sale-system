package com.botter.shop.order.repository;

import com.botter.shop.order.model.Order;
import com.botter.shop.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-09-01 14:33
 * @Description 描述信息
 */
public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {

    List<OrderItem> findOrderItemByOrderId(Long orderId);
}
