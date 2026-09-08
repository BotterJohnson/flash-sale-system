package com.botter.shop.seckill.repository;

import com.botter.shop.seckill.model.SeckillGoods;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeckillGoodsRepository extends JpaRepository<SeckillGoods, Long> {

    Optional<SeckillGoods> findByGoodsId(Long goodsId);

    /**
     * 加行锁读取秒杀活动，扣库存前调用以保证并发安全。
     * 与 @Transactional 配合，未提交事务释放锁。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from SeckillGoods g where g.goodsId = :goodsId")
    Optional<SeckillGoods> findByGoodsIdForUpdate(@Param("goodsId") Long goodsId);
}
