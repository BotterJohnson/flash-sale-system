package com.botter.shop.seckill.mq;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.seckill.config.SeckillMqConfig;
import com.botter.shop.seckill.dto.SeckillMessage;
import com.botter.shop.seckill.model.SeckillGoods;
import com.botter.shop.seckill.model.SeckillOrder;
import com.botter.shop.seckill.repository.SeckillGoodsRepository;
import com.botter.shop.seckill.repository.SeckillOrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    @Autowired
    private SeckillGoodsRepository seckillGoodsRepository;
    @Autowired
    private SeckillOrderRepository seckillOrderRepository;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ObjectMapper objectMapper;

    @RabbitListener(queues = SeckillMqConfig.QUEUE)
    @Transactional   // 直接标在监听方法上，别在同类里再包一层方法（自调用事务不生效）
    public void onMessage(String payload) throws Exception {
        SeckillMessage msg = objectMapper.readValue(payload, SeckillMessage.class);

        try {
            // 行锁扣 DB 库存（消费者串行化，这正是削峰的意义：DB 按自己的节奏写）
            SeckillGoods goods = seckillGoodsRepository.findByGoodsIdForUpdate(msg.goodsId())
                    .orElseThrow(() -> new GlobalException(ResultMsgEnum.SECKILL_NOT_EXIST));

            if (goods.getStock() <= 0) {
                // 兜底：Redis 多扣了（理论上不会），把名额还给 Redis
                compensateRedis(msg);
                return;
            }

            goods.setStock(goods.getStock() - 1);
            seckillGoodsRepository.save(goods);

            SeckillOrder order = new SeckillOrder();
            order.setOrderNo(msg.orderNo());
            order.setUserId(msg.userId());
            order.setSeckillGoodsId(goods.getId());
            order.setGoodsId(goods.getGoodsId());
            order.setGoodsName(goods.getGoodsName());
            order.setPayPrice(goods.getSeckillPrice());
            order.setCreateTime(LocalDateTime.now());
            seckillOrderRepository.save(order);

//            log.info("订单落库成功 orderNo={}, 剩余库存={}", msg.orderNo(), goods.getStock());

        } catch (DataIntegrityViolationException e) {
            // order_no 唯一索引命中 = 重复消息，幂等忽略即可
            log.warn("重复消息，幂等忽略 orderNo={}", msg.orderNo());
        } catch (Exception e) {
            // 落库失败：把 Redis 名额还回去，抛出让 MQ 重试（最多 3 次）
            log.error("订单落库失败，补偿 Redis, orderNo={}", msg.orderNo(), e);
            compensateRedis(msg);
            throw e;
        }
    }

    private void compensateRedis(SeckillMessage msg) {
        redis.opsForValue().increment("seckill:stock:" + msg.goodsId());
        redis.opsForSet().remove("seckill:bought:" + msg.goodsId(), String.valueOf(msg.userId()));
    }
}
