# seckill-service

> 秒杀微服务 — **基础版**，刻意保留性能瓶颈，方便逐步优化 QPS。

## 设计原则

- **最小依赖**：只依赖 `common`、`goods-service-client`（Feign）、`sa-token`、JPA、MySQL。
- **逻辑最简**：先跑通场景，把"分布式锁 / 缓存 / MQ / 限流"全部留作后续优化点。
- **前端完整**：发布、重置、抢购、我的战绩、已抢到标识都已就绪，体验闭环。

## 端口与服务注册

| 项       | 值                                  |
| -------- | ----------------------------------- |
| HTTP     | `18007`                             |
| Eureka 名 | `seckill-service`                    |
| MySQL    | `jdbc:mysql://localhost:33061/seckill`（自动建库） |
| Redis    | `localhost:6379` db `7`（与 user 共享，仅用于 sa-token 会话） |

## 启动

依赖前置服务：**eureka-service**、**user-service**（仅会话）、**goods-service**（发布活动时拉商品）。

```bash
mvn -pl seckill-service -am spring-boot:run
```

Swagger UI：`http://localhost:18007/doc.html`

## 接口列表

| 方法   | 路径                       | 鉴权 | 说明                                |
| ------ | -------------------------- | ---- | ----------------------------------- |
| GET    | `/seckill/list`            | ❌   | 秒杀活动列表（含已购标记）          |
| POST   | `/seckill/do`              | ✅   | 抢购，`?goodsId=...`，返回订单号    |
| POST   | `/seckill/publish`         | ✅   | 发布活动：`goodsId`/`seckillPrice`（分）/`stock`/`delayMinutes`/`durationMinutes` |
| POST   | `/seckill/reset`           | ✅   | 重置活动：`goodsId`/`durationMinutes` |
| GET    | `/seckill/myOrders`        | ✅   | 我的秒杀订单                        |

> 网关对 `/seckill/**` **不** StripPrefix；除 `/seckill/list` 外都需登录。

## 核心逻辑（doSeckill）

```java
@Transactional
public String doSeckill(Long goodsId) {
    long userId = StpUtil.getLoginIdAsLong();

    SeckillGoods goods = seckillGoodsRepository.findByGoodsIdForUpdate(goodsId)  // PESSIMISTIC_WRITE
            .orElseThrow(...);

    // 1. 校验时间 / 库存 / 一人一单
    // 2. stock--，save 活动表
    // 3. save 订单（唯一索引 (user_id, seckill_goods_id) 兜底）
}
```

## 表结构

- `seckill_goods`：秒杀活动
- `seckill_order`：秒杀订单（`UNIQUE(user_id, seckill_goods_id)` 防重复下单）

建表 SQL：`src/main/resources/db/seckill.sql`（JPA `ddl-auto=update` 启动时也会自动建）。

## 已知的 QPS 瓶颈（待你优化）

1. **每次抢购都要走 DB 行锁**，并发上来后锁等待严重 → 可加 Redis 预减库存 + Lua 脚本
2. **`existsByUserIdAndSeckillGoodsId` + `save(order)` 两次查表**，唯一索引已兜底，可省掉前置查询
3. **`publish` 接口同步调用 goods-service**，可加本地缓存或异步化
4. **未读已读未分离**：库存读写都打同一张表 → 可拆 hot/cold 或用 Redis 计数
5. **无任何限流/防刷**：仅靠网关 IP 限流 → 可加令牌桶 / 滑窗 / 验证码
6. **事务粒度偏大**：doSeckill 整个方法一个事务，可缩短到仅 DB 操作

## 演示流程

1. 登录账号 → 拿到 satoken
2. 打开 `http://localhost:18004/seckill.html`
3. 展开"发布 / 重置秒杀活动"，填商品 ID（如 `1`）+ 秒杀价 + 库存，发布
4. 卡片出现"立即抢购"按钮，点击 → 抢购成功弹窗显示订单号
5. 想反复压测？点"重置活动"即可把库存刷回
