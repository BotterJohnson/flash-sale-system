package com.botter.shop.seckill.dto;

/**
 * 秒杀场景下的当前登录用户。
 *
 * <p>由 {@link com.botter.shop.seckill.auth.TokenUserResolver} 从请求头的 satoken
 * 解析而来：userId 来自 Redis 中 token 对应的 loginId，mobile / nickname 来自
 * 登录时写入 Token-Session 的用户快照。
 *
 * <p>只保留秒杀必需字段，不依赖 user-service，也不查 user 库。
 */
public record SeckillUser(Long userId, String mobile, String nickname) {

    /** 用户快照缺失时的兜底展示名 */
    public String displayName() {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (mobile != null && mobile.length() >= 4) {
            return "用户" + mobile.substring(mobile.length() - 4);
        }
        return "用户" + userId;
    }
}
