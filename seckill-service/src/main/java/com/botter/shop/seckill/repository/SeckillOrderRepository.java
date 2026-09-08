package com.botter.shop.seckill.repository;

import com.botter.shop.seckill.model.SeckillOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeckillOrderRepository extends JpaRepository<SeckillOrder, Long> {

    /** 用于"一人一单"判定；与表上的 unique 索引互为兜底 */
    boolean existsByUserIdAndSeckillGoodsId(Long userId, Long seckillGoodsId);

    java.util.List<SeckillOrder> findByUserIdOrderByCreateTimeDesc(Long userId);
}
