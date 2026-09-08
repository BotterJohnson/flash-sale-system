-- =============================================================
-- 秒杀服务建表脚本
-- 说明：服务启动时 JPA(ddl-auto=update) 会自动建表，
--       本脚本仅用于手工执行 / 查看结构。
-- 端口：MySQL 33061（与项目其它服务一致）
-- =============================================================

CREATE DATABASE IF NOT EXISTS seckill
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE seckill;

-- 秒杀活动商品表
CREATE TABLE IF NOT EXISTS seckill_goods
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '秒杀活动id',
    goods_id      BIGINT       NOT NULL COMMENT '商品id（goods-service）',
    goods_name    VARCHAR(127) NOT NULL COMMENT '商品名称（冗余，避免秒杀时回查商品服务）',
    goods_img     VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商品图片',
    origin_price  BIGINT       NOT NULL DEFAULT 0 COMMENT '原价，单位：分',
    seckill_price BIGINT       NOT NULL DEFAULT 0 COMMENT '秒杀价，单位：分',
    total         INT          NOT NULL DEFAULT 0 COMMENT '原始库存（活动总库存，不随抢购减少，用于算已抢=total-stock）',
    stock         INT          NOT NULL DEFAULT 0 COMMENT '当前剩余库存（抢购时递减）',
    start_time    DATETIME     NOT NULL COMMENT '活动开始时间',
    end_time      DATETIME     NOT NULL COMMENT '活动结束时间',
    create_time   DATETIME     NOT NULL COMMENT '创建时间',
    update_time   DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goods_id (goods_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀活动商品';

-- 秒杀订单表
CREATE TABLE IF NOT EXISTS seckill_order
(
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no          VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id           BIGINT      NOT NULL COMMENT '用户id',
    nickname          VARCHAR(45) NULL COMMENT '用户昵称（从 user-service 查询后冗余）',
    seckill_goods_id  BIGINT      NOT NULL COMMENT '秒杀活动id',
    goods_id          BIGINT      NOT NULL COMMENT '商品id',
    goods_name        VARCHAR(127) NULL COMMENT '商品名称',
    pay_price         BIGINT      NOT NULL COMMENT '实付金额，单位：分',
    create_time       DATETIME    NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    -- 一人一单的最后一道防线：即使 Redis 防重失效，数据库也能拦住重复下单
    UNIQUE KEY uk_user_goods (user_id, seckill_goods_id),
    KEY idx_goods (goods_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀订单';
