package com.botter.shop.user.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.common.utils.MD5Util;
import com.botter.shop.user.dao.UserDao;
import com.botter.shop.user.model.User;
import com.botter.shop.user.vo.LoginVo;
import com.botter.shop.user.vo.RegisterVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-31 12:39
 * @Description 用户服务：登录 / 注册
 */
@Service
public class UserService {

    /**
     * token 在前端请求头中携带的名称，与 sa-token 默认 tokenName 保持一致
     */
    public static final String COOKIE_NAME_TOKEN = "satoken";

    @Resource
    private UserDao userDao;

    /**
     * 登录：前端传入的 password 已经做过一次 md5 + 固定盐（formPass），
     * 后端校验时只需用数据库中的随机盐再做一层解析，与库中密文比对即可
     */
    public String login(LoginVo loginVo) {
        if (loginVo == null) {
            throw new GlobalException(ResultMsgEnum.SERVER_ERROR);
        }

        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();

        User user = userDao.getUserByMobile(mobile);
        if (user == null) {
            throw new GlobalException(ResultMsgEnum.MOBILE_NOT_EXIST);
        }

        String dbPass = user.getPassword();
        String saltDB = user.getSalt();

        // 用数据库中的盐对前端密文做二次加密，与库中密文比对
        String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
        if (!calcPass.equals(dbPass)) {
            throw new GlobalException(ResultMsgEnum.PASSWORD_ERROR);
        }

        // 更新登录时间与登录次数
        user.setLastLoginTime(Instant.now());
        user.setLoginCount(user.getLoginCount() == null ? 1L : user.getLoginCount() + 1);
        userDao.save(user);

        // sa-token 登录，有效期 3600 秒
        StpUtil.login(user.getId(), 3600L);

        // 登录成功后缓存用户信息（三种 Session），供跨服务鉴权/取用户
        cacheUserInfo(user);

        return StpUtil.getTokenValue();
    }

    /**
     * 登录成功后缓存用户信息。Sa-Token 的三种 Session：
     * 1) Account-Session：以账号 id 为维度分配
     * 2) Token-Session ：以 token 为维度分配，跨服务最常用
     * 3) Custom-Session ：以自定义字符串作为 SessionId 分配
     */
    private void cacheUserInfo(User user) {
        // 1) Account-Session
        SaSession accountSession = StpUtil.getSession();
        fillSession(accountSession, user);

        // 2) Token-Session：跨服务最常用，order-service 等按 token 维度读取
        SaSession tokenSession = StpUtil.getTokenSession();
        fillSession(tokenSession, user);

        // 3) Custom-Session：以自定义 sessionId 分配，便于按 userId 反查。
        //    注意：StpUtil.getSessionBySessionId(String) 默认 isCreate=false 不会创建，
        //    必须走 StpLogic 的重载并显式传 true 才会新建，否则返回 null 导致 NPE。
        SaSession customSession = StpUtil.getStpLogic()
                .getSessionBySessionId("shop:user:" + user.getId(), true, null, null);
        fillSession(customSession, user);
    }

    /**
     * 填充用户信息。SaSession 内部 dataMap 是 ConcurrentHashMap，不允许 null 值，
     * 因此空字段（如 avatar）不写入，避免登录时抛 NullPointerException。
     */
    private void fillSession(SaSession session, User user) {
        session.set("userId", user.getId());
        if (user.getMobile() != null) {
            session.set("mobile", user.getMobile());
        }
        if (user.getNickname() != null) {
            session.set("nickname", user.getNickname());
        }
        if (user.getAvatar() != null) {
            session.set("avatar", user.getAvatar());
        }
    }

    /**
     * 注册：前端传入的同样是 md5 + 固定盐 的 formPass，
     * 后端生成随机盐，二次加密后入库
     */
    public boolean register(RegisterVo registerVo) {
        if (registerVo == null) {
            throw new GlobalException(ResultMsgEnum.SERVER_ERROR);
        }

        String mobile = registerVo.getMobile();
        if (userDao.getUserByMobile(mobile) != null) {
            throw new GlobalException(ResultMsgEnum.MOBILE_EXIST);
        }

        String saltDB = MD5Util.randomSalt();
        String dbPass = MD5Util.formPassToDBPass(registerVo.getPassword(), saltDB);

        User user = new User();
        user.setMobile(mobile);
        user.setNickname(registerVo.getNickname() == null || registerVo.getNickname().isBlank()
                ? "用户" + mobile.substring(mobile.length() - 4)
                : registerVo.getNickname());
        user.setPassword(dbPass);
        user.setSalt(saltDB);
        Instant now = Instant.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setLastLoginTime(now);
        user.setLoginCount(0L);
        user.setPoint(0L);

        userDao.save(user);
        return true;
    }

    /**
     * 根据用户 id 获取用户；id 由 Sa-Token 从请求头/Cookie 中解析得到的当前登录用户
     */
    public User getUserById(Long userId) {
        if (userId == null) {
            throw new GlobalException(ResultMsgEnum.SESSION_ERROR);
        }
        return userDao.findById(userId)
                .orElseThrow(() -> new GlobalException(ResultMsgEnum.USER_NOT_EXIST));
    }

    /**
     * 增加用户积分
     */
    public User addPoint(Long userId, Integer point) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new GlobalException(ResultMsgEnum.USER_NOT_EXIST));
        long cur = user.getPoint() == null ? 0L : user.getPoint();
        user.setPoint(cur + point);
        user.setUpdateTime(Instant.now());
        return userDao.save(user);
    }

    /**
     * 事务测试：两次更新在同一事务内，任一失败整体回滚
     */
    @Transactional
    public boolean transaction() {
        User user = userDao.findById(1L).orElse(null);
        if (user == null) {
            return false;
        }
        user.setPoint((user.getPoint() == null ? 0L : user.getPoint()) + 1);
        user.setUpdateTime(Instant.now());
        userDao.save(user);
        return true;
    }
}
