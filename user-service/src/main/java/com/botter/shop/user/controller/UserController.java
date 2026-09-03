package com.botter.shop.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.user.model.User;
import com.botter.shop.user.service.UserService;
import com.botter.shop.user.vo.LoginVo;
import com.botter.shop.user.vo.RegisterVo;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    // 网关 StripPrefix=1 会剥掉一级 /user，转发到本服务的路径为 /login/dologin
    @RequestMapping("/login/dologin")
    @ResponseBody
    public Result<String> doLogin(@Valid LoginVo loginVo) {
        logger.info(loginVo.toString());
        //登录，前端传入的 password 已是 md5 + 固定盐的密文
        String token = userService.login(loginVo);
        return Result.success(token);
    }

    // 网关转发路径为 /register/register
    @RequestMapping("/register/register")
    @ResponseBody
    public Result<String> doRegister(@Valid RegisterVo registerVo) {
        logger.info(registerVo.toString());
        //注册，后端生成随机盐，二次加密后入库
        userService.register(registerVo);
        return Result.success("注册成功");
    }

    // 网关转发路径为 /info
    @RequestMapping("/info")
    @ResponseBody
    public Result<User> getUserInfo() {
        // 使用 Sa-Token 自动读取 token：兼容请求头(satoken)与 Cookie 两种携带方式
        long userId = StpUtil.getLoginIdAsLong();
        return Result.success(userService.getUserById(userId));
    }

    // 网关转发路径为 /logout
    @RequestMapping("/logout")
    @ResponseBody
    public Result<String> logout() {
        StpUtil.logout();
        return Result.success("退出成功");
    }

    @GetMapping("/addPoint")
    @ResponseBody
    public Result<User> addPoint(@RequestParam Long userId, @RequestParam Integer point) {
        return Result.success(userService.addPoint(userId, point));
    }

    @GetMapping("/testTx")
    @ResponseBody
    public Result<String> testTx(){
        if (userService.transaction()) {
            return Result.success("success");
        }
        return Result.error(ResultMsgEnum.SERVER_ERROR);
    }
}
