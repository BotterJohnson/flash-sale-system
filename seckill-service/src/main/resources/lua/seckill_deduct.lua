-- 秒杀原子扣减：token 校验 + 一人一单 + 库存预扣，一次网络往返完成
-- KEYS[1] = seckill:bought:<goodsId>
-- KEYS[2] = seckill:stock:<goodsId>
-- KEYS[3] = satoken:login:token:<token>
-- 返回数组 {状态码, loginId}
--   1  = 受理成功
--   0  = 重复秒杀
--   -1 = 已售罄
--   -2 = token 无效或过期

local loginId = redis.call('GET', KEYS[3])
if not loginId then
    return {'-2', ''}
end

-- 一人一单：SADD 返回 0 说明 userId 已在集合里，直接拒绝
if redis.call('SADD', KEYS[1], loginId) == 0 then
    return {'0', tostring(loginId)}
end

-- 库存预扣：扣成负数说明卖超，脚本内立刻原地回滚，不用 Java 再补偿
local left = redis.call('DECR', KEYS[2])
if left < 0 then
    redis.call('INCR', KEYS[2])
    redis.call('SREM', KEYS[1], loginId)
    return {'-1', tostring(loginId)}
end

return {'1', tostring(loginId)}
