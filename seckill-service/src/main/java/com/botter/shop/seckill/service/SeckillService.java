package com.botter.shop.seckill.service;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.goods.api.GoodsApi;
import com.botter.shop.goods.dto.GoodsDTO;
import com.botter.shop.seckill.auth.TokenUserResolver;
import com.botter.shop.seckill.config.SeckillMqConfig;
import com.botter.shop.seckill.dto.*;
import com.botter.shop.seckill.model.SeckillGoods;
import com.botter.shop.seckill.model.SeckillOrder;
import com.botter.shop.seckill.repository.SeckillGoodsRepository;
import com.botter.shop.seckill.repository.SeckillOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秒杀服务（基础版）。
 *
 * <p>逻辑尽量简单：
 * <ul>
 *   <li>行锁（PESSIMISTIC_WRITE）+ 事务：扣库存原子化，避免超卖</li>
 *   <li>"一人一单"靠表上唯一索引兜底，应用层先查一遍给友好提示</li>
 *   <li>暂不引入 Redis 库存预减 / 消息队列，目标是先跑通场景</li>
 * </ul>
 *
 * <p>精度说明：后端 id 一律用 {@code Long}（19 位雪花 < Long.MAX=9.2e18，能装下），
 * 真正的精度坑在浏览器端，<b>前端必须按字符串拼接与传递</b>，否则 JS Number 会丢精度。
 */
@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    @Autowired
    private SeckillGoodsRepository seckillGoodsRepository;

    @Autowired
    private SeckillOrderRepository seckillOrderRepository;

    @Autowired
    private GoodsApi goodsApi;

    /**
     * 引入redis 和 mq
     * 
     */
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PublishService publishService;

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";   // 库存计数器
    private static final String BOUGHT_KEY_PREFIX = "seckill:bought:"; // 已购用户 Set（一人一单）
    private static final String INFO_KEY_PREFIX = "seckill:info:";     // 活动信息快照（publishService/reset 时预热）

    @Autowired
    private DefaultRedisScript<List> seckillDeductScript;

    private final Map<Long, Boolean> SOLD_OUT = new ConcurrentHashMap<>();

    // ==================== 查询 ====================

    /** 秒杀列表（游客也能看，登录后多了 bought/orderId 标记） */
    public List<SeckillGoodsVO> list(SeckillUser user) {
        Map<Long, SeckillOrder> boughtMap = new HashMap<>();
        if (user != null) {
            for (SeckillOrder order : seckillOrderRepository.findByUserIdOrderByCreateTimeDesc(user.userId())) {
                boughtMap.put(order.getSeckillGoodsId(), order);
            }
        }
        List<SeckillGoodsVO> list = new ArrayList<>();
        for (SeckillGoods goods : seckillGoodsRepository.findAll()) {
            list.add(toVO(goods, boughtMap.get(goods.getId())));
        }
        return list;
    }

    /** 我的秒杀订单 */
    public List<SeckillOrderVO> myOrders(SeckillUser user) {
        List<SeckillOrderVO> list = new ArrayList<>();
        for (SeckillOrder order : seckillOrderRepository.findByUserIdOrderByCreateTimeDesc(user.userId())) {
            list.add(toOrderVO(order));
        }
        return list;
    }

    // ==================== 秒杀 ====================

    /**
     * 执行秒杀，返回订单号。
     *
     * <p>前端必须按字符串传 goodsId（19 位雪花 > JS Number 安全整数上限），
     * Spring MVC 会用 Long.parseLong 安全转为 {@code Long}，全程不丢精度。
     */
    @Transactional
    public String doSeckill(Long goodsId, SeckillUser user) {
        long userId = user.userId();
        log.info("秒杀请求 goodsId={}, user={}({})", goodsId, userId, user.displayName());

        var goods = getGoods(goodsId, userId);

        goods.setStock(goods.getStock() - 1);
        seckillGoodsRepository.save(goods);  //可能出现库存超卖

        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(genOrderNo(userId));
        order.setUserId(userId);
        order.setSeckillGoodsId(goods.getId());
        order.setGoodsId(goods.getGoodsId());
        order.setGoodsName(goods.getGoodsName());
        order.setPayPrice(goods.getSeckillPrice());
        order.setCreateTime(LocalDateTime.now());
        try {
            seckillOrderRepository.save(order);
        } catch (DataIntegrityViolationException e) {
            throw new GlobalException(ResultMsgEnum.SECKILL_REPEAT);
        }
        log.info("秒杀成功 userId={}, goodsId={}, orderNo={}, 剩余库存={}", userId, goodsId, order.getOrderNo(), goods.getStock());
        return order.getOrderNo();
    }

    private @NonNull SeckillGoods getGoods(Long goodsId, long userId) {
        //先看秒杀活动是否存在
        SeckillGoods goods = seckillGoodsRepository.findByGoodsIdForUpdate(goodsId)
                .orElseThrow(() -> new GlobalException(ResultMsgEnum.SECKILL_NOT_EXIST));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(goods.getStartTime())) { //如果秒杀活动未开始
            throw new GlobalException(ResultMsgEnum.SECKILL_NOT_START);
        }
        if (now.isAfter(goods.getEndTime())) { //如果秒杀活动已结束
            throw new GlobalException(ResultMsgEnum.SECKILL_END);
        }
        if (goods.getStock() <= 0) { //如果秒杀活动已售罄
            throw new GlobalException(ResultMsgEnum.SECKILL_SOLD_OUT);
        }
        if (seckillOrderRepository.existsByUserIdAndSeckillGoodsId(userId, goods.getId())) { //如果用户已秒杀过该商品
            throw new GlobalException(ResultMsgEnum.SECKILL_REPEAT);
        }
        return goods;
    }

    /**
     * Redis + MQ 版秒杀：请求路径全程不碰 DB。
     *
     * <p>关键点：活动校验读 publishService/reset 时预热进 Redis 的快照（seckill:info:*），
     * 绝不能走 getGoods() 的 SELECT ... FOR UPDATE——那会让 10000 个请求
     * 在一行行锁上串行排队，Redis 预减 + MQ 削峰就全白做了。
     */
    public String doSeckillByRedis(Long goodsId, SeckillUser user) {
        var userId = user.userId();

        // ① 活动校验：读 Redis 快照（存在性 + 时间窗口），不查库
        SeckillActivityCache cache = readActivityCache(goodsId);
        long now = System.currentTimeMillis();
        if (now < cache.startTime()) {
            throw new GlobalException(ResultMsgEnum.SECKILL_NOT_START);
        }
        if (now > cache.endTime()) {
            throw new GlobalException(ResultMsgEnum.SECKILL_END);
        }

        // ② 一人一单：SADD 原子占位。注意 key 必须拼 goodsId 字符串，不能拼实体对象！
        var first = redis.opsForSet().add(BOUGHT_KEY_PREFIX + goodsId, String.valueOf(userId) );
        if (first == null || first == 0) {
            throw new GlobalException(ResultMsgEnum.SECKILL_REPEAT);
        }

        // ③ 库存预减：DECR 原子扣减，扣成负数说明卖超了，回滚占位
        Long left = redis.opsForValue().decrement(STOCK_KEY_PREFIX + goodsId);
        if (left == null || left < 0) {
            rollbackRedis(goodsId, userId);
            throw new GlobalException(ResultMsgEnum.SECKILL_SOLD_OUT);
        }

        // ④ 订单号在生产端预生成（消费端幂等键），发 MQ 异步落库
        String orderNo = genOrderNo(userId);
        try {
            String payload = objectMapper.writeValueAsString(new SeckillMessage(orderNo, goodsId, userId));
            rabbitTemplate.convertAndSend(SeckillMqConfig.EXCHANGE, SeckillMqConfig.ROUTING_KEY, payload);
        } catch (Exception e) {
            // 发送失败必须回滚 Redis，否则这个名额就凭空蒸发了
            log.error("MQ 发送失败，回滚 Redis, goodsId={}, userId={}", goodsId, userId, e);
            rollbackRedis(goodsId, userId);
            throw new GlobalException(ResultMsgEnum.SECKILL_STOCK_FAIL);
        }
        log.info("秒杀请求受理 userId={}, goodsId={}, orderNo={}, Redis剩余={}", userId, goodsId, orderNo, left);
        return orderNo;
    }

    /** 读取发布时预热的活动快照；没有快照说明活动未发布（或 Redis 被清了） */
    private SeckillActivityCache readActivityCache(Long goodsId) {
        String json = redis.opsForValue().get(INFO_KEY_PREFIX + goodsId);
        if (json == null) {
            throw new GlobalException(ResultMsgEnum.SECKILL_NOT_EXIST);
        }
        try {
            return objectMapper.readValue(json, SeckillActivityCache.class);
        } catch (JsonProcessingException e) {
            log.error("活动快照解析失败 goodsId={}", goodsId, e);
            throw new GlobalException(ResultMsgEnum.SERVER_ERROR);
        }
    }
    /** 补偿：把预扣的库存和占位还回去 */
    private void rollbackRedis(Long goodsId, long userId) {
        redis.opsForValue().increment(STOCK_KEY_PREFIX + goodsId);
        redis.opsForSet().remove(BOUGHT_KEY_PREFIX + goodsId, String.valueOf(userId));
    }

    // ==================== 活动管理 ====================

    /**
     * 发布秒杀活动：从 goods-service 拉商品信息并落库。
     * delayMinutes=0 表示立即开始，durationMinutes 默认 30 分钟。
     */
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
        // total 记录原始库存（活动总库存），stock 是当前剩余，二者分离才能算"已抢 = total - stock"
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
        log.info("发布秒杀活动 seckillGoodsId={}, goodsId={}, stock={}", saved.getId(), goodsId, stock);
        return toVO(saved, null);
    }

    /**
     * 重置活动：把库存刷回原始库存（total）并重新计时，用于反复演示/压测。
     * 注意：只重置 current stock，不动 total（total 是一次发布定的"原始库存"）。
     */
    public SeckillGoodsVO reset(Long goodsId, Integer durationMinutes) {
        SeckillGoods goods = seckillGoodsRepository.findByGoodsId(goodsId)
                .orElseThrow(() -> new GlobalException(ResultMsgEnum.SECKILL_NOT_EXIST));
        LocalDateTime now = LocalDateTime.now();
        // 库存刷回初始值（total 是发布的原始库存，重置后 stock 回到 total，已抢量归零）
        goods.setStock(goods.getTotal());
        goods.setStartTime(now);
        goods.setEndTime(now.plusMinutes(durationMinutes == null || durationMinutes <= 0 ? 30 : durationMinutes));
        goods.setUpdateTime(now);
        SeckillGoods saved = seckillGoodsRepository.save(goods);
        // 重置也要重新预热：库存计数器刷回、已购 Set 清空、活动时间窗快照更新
        publishService.warmUpRedis(saved);
        return toVO(saved, null);
    }

    // ==================== 私有方法 ====================

    private String genOrderNo(Long userId) {
        return "SK" + System.currentTimeMillis() + String.format("%04d", userId % 10000);
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

    public String doSeckillByRedisLua(Long goodsId, String token) { // ① 活动校验：读 publish/reset 预热的快照（同旧逻辑）
        readActivityCache(goodsId);

        if (Boolean.TRUE.equals(SOLD_OUT.get(goodsId))) {
            throw new GlobalException(ResultMsgEnum.SECKILL_SOLD_OUT);
        }
        // ② 一次往返完成 token 校验 + SADD 占位 + DECR 扣减
        List<String> r;
        try {
            r = redis.execute(seckillDeductScript,
                    List.of(
                            BOUGHT_KEY_PREFIX + goodsId,
                            STOCK_KEY_PREFIX + goodsId,
                            TokenUserResolver.TOKEN_KEY_PREFIX + token));
        } catch (Exception e) {
            log.error("Lua 脚本执行失败 goodsId={}", goodsId, e);
            throw new GlobalException(ResultMsgEnum.SECKILL_STOCK_FAIL);
        }

        // ③ 按状态码翻译结果
        String code = r.get(0);
        String loginId = r.get(1);
        switch (code) {
            case "-2" -> throw new GlobalException(ResultMsgEnum.SESSION_ERROR);
            case "0"  -> throw new GlobalException(ResultMsgEnum.SECKILL_REPEAT);
            case "-1" -> {
                SOLD_OUT.put(goodsId, true);
                throw new GlobalException(ResultMsgEnum.SECKILL_SOLD_OUT);
            }
        }
        long userId = Long.parseLong(loginId);

        // ④ 发 MQ 异步落库（同旧逻辑；发送失败仍需 Java 侧回滚）
        String orderNo = genOrderNo(userId);
        try {
            String payload = objectMapper.writeValueAsString(new SeckillMessage(orderNo, goodsId, userId));
            rabbitTemplate.convertAndSend(SeckillMqConfig.EXCHANGE, SeckillMqConfig.ROUTING_KEY, payload);
        } catch (Exception e) {
            log.error("MQ 发送失败，回滚 Redis, goodsId={}, userId={}", goodsId, userId, e);
            redis.opsForValue().increment(STOCK_KEY_PREFIX + goodsId);
            redis.opsForSet().remove(BOUGHT_KEY_PREFIX + goodsId, loginId);
            throw new GlobalException(ResultMsgEnum.SECKILL_STOCK_FAIL);
        }
        log.info("秒杀请求受理 userId={}, goodsId={}, orderNo={}", userId, goodsId, orderNo);
        return orderNo;
    }
}
