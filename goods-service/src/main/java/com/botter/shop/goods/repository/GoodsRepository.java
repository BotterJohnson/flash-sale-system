package com.botter.shop.goods.repository;

import com.botter.shop.goods.model.Brand;
import com.botter.shop.goods.model.Goods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 21:01
 * @Description 描述信息
 */
public interface GoodsRepository extends JpaRepository<Goods, Long> {

    //根据状态过滤商品信息
    List<Goods> findByStatus(Integer status);

    List<Goods> getByStatus(int i);

    @Modifying
    @Query(value = "UPDATE goods SET stock = stock - :cost WHERE id = :id AND stock >= :cost",
            nativeQuery = true)
    int decrStock(@Param("id") Long goodsId, @Param("cost") Integer cost);
}
