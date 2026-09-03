package com.botter.shop.order.repository;

import com.botter.shop.order.model.CartItem;
import com.botter.shop.order.model.Order;
import com.botter.shop.order.vo.OrdersParam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-09-01 14:33
 * @Description 描述信息
 */
public interface OrderRepository extends JpaRepository<Order,Long> {

    List<Order> findByUserId(Long userId);
}
