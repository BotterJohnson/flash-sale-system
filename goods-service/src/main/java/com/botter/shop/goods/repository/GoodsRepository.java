package com.botter.shop.goods.repository;

import com.botter.shop.goods.model.Brand;
import com.botter.shop.goods.model.Goods;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 21:01
 * @Description 描述信息
 */
public interface GoodsRepository extends JpaRepository<Goods, Long> {
}
