package com.botter.shop.goods.repository;

import com.botter.shop.goods.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 21:00
 * @Description 描述信息
 */
public interface CategoryRepository extends JpaRepository<Category,Long> {
}
