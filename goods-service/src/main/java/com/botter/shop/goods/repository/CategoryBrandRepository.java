package com.botter.shop.goods.repository;

import com.botter.shop.goods.model.CategoryBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 20:59
 * @Description 描述信息
 */
public interface CategoryBrandRepository extends JpaRepository<CategoryBrand, Long> {
    List<CategoryBrand> findByCategoryId(long attr0);
}
