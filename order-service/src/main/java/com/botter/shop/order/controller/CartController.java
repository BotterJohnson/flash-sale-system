package com.botter.shop.order.controller;

 
import com.botter.shop.common.result.Result;
import com.botter.shop.order.access.AccessLimit;
import com.botter.shop.order.access.UserContext;
import com.botter.shop.order.service.CartService;
import com.botter.shop.order.vo.CartItemVO;
import com.botter.shop.order.vo.CartSizeVO;
import com.botter.shop.order.vo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    CartService cartService;
    
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("ok_test");
    }

    // 跨服务验证入口：登录后由 AccessInterceptor 从 token 解析用户并写入 UserContext
    @GetMapping("/me")
    public Result<User> me() {
        return Result.success(UserContext.getUser());
    }

    @GetMapping("/incr")
    @AccessLimit
    public Result<String> incr(User user,
                               @RequestParam Long goodsId) {
        int userId = user.getId();
        cartService.incr(userId, goodsId);
        return Result.success("success");
    }

    @GetMapping("/decr")
    @AccessLimit
    public Result<String> decr(@RequestParam Long goodsId,
                               User user) {
        int userId = user.getId();
        cartService.decr(userId, goodsId);
        return Result.success("success");
    }

    @GetMapping("/set")
    @AccessLimit
    public Result<String> set(@RequestParam Long goodsId,
                              @RequestParam Integer count,
                              User user) {
        cartService.setCount(user.getId(), goodsId, count);
        return Result.success("success");
    }
//
    @GetMapping("/list")
    @AccessLimit
    public Result<List<CartItemVO>> list(User user) {
        return Result.success(cartService.list(user.getId()));
    }

    /**
     * 购物车数量统计：kinds = 商品种类数，total = 商品总件数。
     * 前端导航栏角标以此接口为准，避免本地累加与真实数据不一致。
     */
    @GetMapping("/size")
    @AccessLimit
    public Result<CartSizeVO> size(User user) {
        return Result.success(cartService.countSize(user.getId()));
    }
}
