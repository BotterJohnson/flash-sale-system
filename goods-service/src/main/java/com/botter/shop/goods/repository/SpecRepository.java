package com.botter.shop.goods.repository;

import com.botter.shop.goods.model.Spec;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 21:02
 * @Description 描述信息
 */
public interface SpecRepository extends JpaRepository<Spec, Long> {
}
