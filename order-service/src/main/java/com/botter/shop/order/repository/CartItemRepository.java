package com.botter.shop.order.repository;

import com.botter.shop.order.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-09-01 14:33
 * @Description 描述信息
 */
public interface CartItemRepository extends JpaRepository<CartItem,Long> {

    CartItem findByUserIdAndGoodsId(long userId, long goodsId);

    void deleteByUserIdAndGoodsId(long userId, long goodsId);

    List<CartItem> findByUserId(long userId);

}
