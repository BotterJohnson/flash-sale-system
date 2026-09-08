package com.botter.shop.seckill.service;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.goods.api.GoodsApi;
import com.botter.shop.goods.dto.GoodsDTO;
import com.botter.shop.seckill.dto.SeckillActivityCache;
import com.botter.shop.seckill.dto.SeckillGoodsVO;
import com.botter.shop.seckill.dto.SeckillOrderVO;
import com.botter.shop.seckill.model.SeckillGoods;
import com.botter.shop.seckill.model.SeckillOrder;
import com.botter.shop.seckill.repository.SeckillGoodsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

@Service
/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-09-05 15:54
 * @Description 描述信息
 */
public class PublishService {
    private static final String INFO_KEY_PREFIX = "seckill:info:";
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String BOUGHT_KEY_PREFIX = "seckill:bought:";
    private static final Logger log = LoggerFactory.getLogger(PublishService.class);
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private GoodsApi goodsApi;
    @Autowired
    private SeckillGoodsRepository seckillGoodsRepository;

    

    public SeckillGoodsVO publish(Long goodsId, Long seckillPrice, Integer stock,
                                  Integer delayMinutes, Integer durationMinutes) {
        
        Result<GoodsDTO> goodsResult = goodsApi.get(goodsId);
        if (goodsResult == null || goodsResult.getCode() != 200 || goodsResult.getData() == null) {
            throw new GlobalException(ResultMsgEnum.GOODS_NOT_EXIST);
        }
        GoodsDTO goodsDTO = goodsResult.getData();

        LocalDateTime now = LocalDateTime.now();
        SeckillGoods goods = seckillGoodsRepository.findByGoodsId(goodsId).orElse(new SeckillGoods());
        goods.setGoodsId(goodsId);
        goods.setGoodsName(goodsDTO.name());
        goods.setGoodsImg(goodsDTO.image() == null ? "" : goodsDTO.image());
        goods.setOriginPrice(goodsDTO.price() == null ? 0L : goodsDTO.price());
        goods.setSeckillPrice(seckillPrice);
        goods.setTotal(stock);
        goods.setStock(stock);
        goods.setStartTime(now.plusMinutes(delayMinutes == null || delayMinutes < 0 ? 0 : delayMinutes));
        goods.setEndTime(goods.getStartTime()
                .plusMinutes(durationMinutes == null || durationMinutes <= 0 ? 30 : durationMinutes));
        if (goods.getCreateTime() == null) {
            goods.setCreateTime(now);
        }
        goods.setUpdateTime(now);
        SeckillGoods saved = seckillGoodsRepository.save(goods);

        // ===== 预热：发布即写 Redis，之后秒杀请求全程不查库 =====
        warmUpRedis(saved);

        log.info("发布秒杀活动 seckillGoodsId={}, goodsId={}, stock={}，Redis 预热完成",
                saved.getId(), goodsId, stock);
        return toVO(saved, null);
    }

    private SeckillGoodsVO toVO(SeckillGoods goods, SeckillOrder order) {
        return new SeckillGoodsVO(
                goods.getId(),
                goods.getGoodsId(),
                goods.getGoodsName(),
                goods.getGoodsImg(),
                goods.getOriginPrice(),
                goods.getSeckillPrice(),
                // stock 是当前剩余；total 是原始库存，二者分离前端才能算"已抢 = total - stock"
                Math.max(0, goods.getStock()),
                Math.max(goods.getTotal() == null ? goods.getStock() : goods.getTotal(), 0),
                statusOf(goods),
                toMillis(goods.getStartTime()),
                toMillis(goods.getEndTime()),
                order != null,
                order == null ? null : order.getOrderNo(),
                System.currentTimeMillis());
    }

    /**
     * 发布/重置共用的预热：三件事
     * ① 活动信息快照（存在性 + 时间窗口校验的数据源）
     * ② 库存计数器（DECR 扣减的数据源）
     * ③ 清空上一轮已购集合（新活动/重置后允许重新抢）
     */
    public void warmUpRedis(SeckillGoods goods) {
        long expireTime = 10000;
        try {
            SeckillActivityCache cache = new SeckillActivityCache(
                    goods.getId(), goods.getGoodsId(), goods.getGoodsName(),
                    goods.getSeckillPrice(),
                    goods.getStartTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli(),
                    goods.getEndTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
            expireTime = goods.getEndTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
                    - goods.getStartTime().toInstant(ZoneOffset.ofHours(8)).toEpochMilli() + 1000;
            redis.opsForValue().set(INFO_KEY_PREFIX + goods.getGoodsId(),
                    objectMapper.writeValueAsString(cache) , 
                    expireTime , TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            throw new GlobalException(ResultMsgEnum.SERVER_ERROR);
        }
        redis.opsForValue().set(STOCK_KEY_PREFIX + goods.getGoodsId(), String.valueOf(goods.getStock()) ,expireTime , TimeUnit.MILLISECONDS);
        redis.delete(BOUGHT_KEY_PREFIX + goods.getGoodsId());
    }

    private int statusOf(SeckillGoods goods) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(goods.getStartTime())) { //如果秒杀活动未开始
            return 0;
        }
        if (now.isAfter(goods.getEndTime())) {  //如果秒杀活动已结束
            return 2;
        }
        return 1; //如果秒杀活动进行中
    }

    private SeckillOrderVO toOrderVO(SeckillOrder order) {
        return new SeckillOrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getSeckillGoodsId(),
                order.getGoodsId(),
                order.getGoodsName(),
                order.getPayPrice(),
                order.getCreateTime() == null ? null : toMillis(order.getCreateTime()));
    }

    private Long toMillis(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }

}
