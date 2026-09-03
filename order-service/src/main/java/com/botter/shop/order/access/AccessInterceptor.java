package com.botter.shop.order.access;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.botter.shop.order.vo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-09-01 16:32
 * @Description 跨服务登录校验：从请求头 satoken / Cookie 中解析 token，
 *              校验通过后把登录时缓存的用户信息写入 UserContext
 */
@Service
public class AccessInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AccessInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        logger.debug("========== AccessInterceptor preHandle ==========");
        if (handler instanceof HandlerMethod) {
            // 1) Sa-Token 登录校验：token 从请求头 satoken 或 Cookie 中自动解析
            StpUtil.checkLogin();

            // 2) 从 Token-Session 读取登录时缓存到用户信息
            User user = buildUserFromTokenSession();
            UserContext.setUser(user);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal，防止线程复用导致用户串号
        UserContext.remove();
    }

    /**
     * 从当前请求对应的 Token-Session 中读取登录时缓存的用户信息。
     * 也可以改用 Account-Session：StpUtil.getSession()，按账号 id 维度读取。
     */
    private User buildUserFromTokenSession() {
        SaSession session = StpUtil.getTokenSession();
        User user = new User();

        // 兜底：loginId 一定来自 token，即使 Session 未缓存也能拿到用户 id
        user.setId((int) StpUtil.getLoginIdAsLong());

        Object userId = session.get("userId");
        if (userId instanceof Number) {
            user.setId(((Number) userId).intValue());
        }
        user.setMobile(asString(session.get("mobile")));
        user.setNickname(asString(session.get("nickname")));
        user.setAvatar(asString(session.get("avatar")));
        return user;
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}

