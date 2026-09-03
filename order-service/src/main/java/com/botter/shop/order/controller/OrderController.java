package com.botter.shop.order.controller;


import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.CodeMsg;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.order.access.AccessLimit;
import com.botter.shop.order.model.Order;
import com.botter.shop.order.model.TestVO;
import com.botter.shop.order.repository.OrderRepository;
import com.botter.shop.order.service.OrderService;
import com.botter.shop.order.vo.OrderVO;
import com.botter.shop.order.vo.OrdersParam;
import com.botter.shop.order.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    OrderService orderService;

    @GetMapping("/get/{id}")
    public Result<TestVO> testGet(@PathVariable(name = "id")Long id) {
        if (id > 10) {
            throw new GlobalException(ResultMsgEnum.SERVER_ERROR);
        }
        return Result.success(new TestVO());
    }

    @PostMapping("/genOrder")
    @AccessLimit
    public Result<Order> genOrder(User user, @RequestBody OrdersParam ordersParam) {
        return Result.success(orderService.genOrder(user.getId(), ordersParam));
    }

    @GetMapping("/getOrder/{orderId}")
    @AccessLimit
    public Result<OrderVO> getOrderById(User user, @PathVariable String orderId) {
        return Result.success(orderService.getOrderById(Long.parseLong(orderId)));
    }

    @GetMapping("/list")
    @AccessLimit
    public Result<List<OrderVO>> listOrders(User user) {
        return Result.success(orderService.listOrders(Long.valueOf(user.getId())));
    }

}
