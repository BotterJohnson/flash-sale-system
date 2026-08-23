package com.botter.shop.goods.repository;

import com.botter.shop.goods.model.Brand;
import org.hibernate.annotations.SQLInsert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 20:55
 * @Description 描述信息
 */

@Repository
public interface BrandRepository extends JpaRepository<Brand , Long> {

}
