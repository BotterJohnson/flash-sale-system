package com.botter.shop.seckill.auth;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.seckill.dto.SeckillUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 从请求头的 satoken 解析当前登录用户。
 *
 * <p>不经过 Sa-Token 的 {@code StpUtil}，而是自己走一遍 Redis，逻辑完全显式：
 * <ol>
 *   <li>从请求头 {@code satoken} 取 token，缺失直接报 TOKEN_EMPTY</li>
 *   <li>查 Redis key {@code satoken:login:token:<token>}，值就是 loginId（即 userId）。
 *       查不到说明 token 伪造或已过期，报 SESSION_ERROR</li>
 *   <li>再查 {@code satoken:login:token-session:<token>}，从 dataMap 里取
 *       mobile / nickname 快照，补全用户实体</li>
 * </ol>
 *
 * <p>关于 key 前缀：这是 sa-token 1.46 的 Redis 存储约定（实测确认）：
 * <pre>
 *   satoken:login:token:&lt;token&gt;          -> "1557"                  token -> loginId
 *   satoken:login:token-session:&lt;token&gt;  -> SaSession JSON（含 dataMap）
 *   satoken:login:session:&lt;loginId&gt;      -> Account-Session JSON
 * </pre>
 * 前缀可以通过 {@code sa-token.token-prefix} 改，本项目没配所以用默认值。
 * 升级 sa-token 大版本时记得回来核对这几个 key。
 *
 * <p>网关的 {@code AuthWebFilter} 已经做过一遍登录校验，这里是第二道：
 * 直连 18007 压测时没有网关，全靠这里兜底。
 */
@Component
public class TokenUserResolver {

    private static final Logger log = LoggerFactory.getLogger(TokenUserResolver.class);

    /** 请求头中携带 token 的名称，必须与 user-service / 网关的 sa-token.token-name 一致 */
    public static final String TOKEN_HEADER = "satoken";

    public static final String TOKEN_KEY_PREFIX = "satoken:login:token:";
    private static final String TOKEN_SESSION_PREFIX = "satoken:login:token-session:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public TokenUserResolver(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * 解析当前登录用户，未登录或 token 失效时抛业务异常。
     * 用于 /seckill/do、/seckill/myOrders 这类必须登录的接口。
     */
    public SeckillUser resolve(HttpServletRequest request) {
        String token = readToken(request);
        if (token == null || token.isBlank()) {
            throw new GlobalException(ResultMsgEnum.TOKEN_EMPTY);
        }

        // 第一步：token 是否还在 Redis 里，值就是 userId
        String loginId = redis.opsForValue().get(TOKEN_KEY_PREFIX + token);
        if (loginId == null || loginId.isBlank()) {
            log.warn("token 无效或已过期: {}", token);
            throw new GlobalException(ResultMsgEnum.SESSION_ERROR);
        }

        Long userId;
        try {
            userId = Long.parseLong(loginId);
        } catch (NumberFormatException e) {
            log.error("token 对应的 loginId 非法: {} -> {}", token, loginId);
            throw new GlobalException(ResultMsgEnum.SESSION_ERROR);
        }

        // 第二步：读一次 Token-Session，把 mobile / nickname 快照补全；取不到也不影响主流程
        String sessionJson = loadSessionJson(token);
        return new SeckillUser(userId, readField(sessionJson, "mobile"),
                readField(sessionJson, "nickname"));
    }

    /** 读 Token-Session 原文，只查一次 Redis */
    private String loadSessionJson(String token) {
        try {
            return redis.opsForValue().get(TOKEN_SESSION_PREFIX + token);
        } catch (Exception e) {
            log.warn("读取 Token-Session 失败, token={}, err={}", token, e.toString());
            return null;
        }
    }

    /**
     * 允许游客访问的场景用这个：解析失败返回 null，不抛异常。
     * 用于 /seckill/list（游客可浏览，登录后多一个「已抢到」标记）。
     */
    public SeckillUser resolveOrNull(HttpServletRequest request) {
        try {
            return resolve(request);
        } catch (GlobalException e) {
            log.debug("游客登录异常,错误信息{}", e.getCm().getMsg());
            return null;
        }
    }

    /** 优先取请求头，其次 Cookie（浏览器直连时用得上） */
    private String readToken(HttpServletRequest request) {
        String token = request.getHeader(TOKEN_HEADER);
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (TOKEN_HEADER.equals(cookie.getName()) && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue().trim();
                }
            }
        }
        return null;
    }

    /** 从已取到的 Token-Session 原文里读 dataMap 的一个字段，取不到返回 null */
    private String readField(String sessionJson, String field) {
        if (sessionJson == null || sessionJson.isBlank()) {
            return null;
        }
        try {
            JsonNode dataMap = objectMapper.readTree(sessionJson).path("dataMap");
            JsonNode node = dataMap.get(field);
            return node == null || node.isNull() ? null : node.asText();
        } catch (Exception e) {
            log.warn("解析 Token-Session 失败, field={}, err={}", field, e.toString());
            return null;
        }
    }
}
