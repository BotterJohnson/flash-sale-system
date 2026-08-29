package com.botter.shop.canal.controller;

import com.botter.shop.canal.rabbitmq.MqReceiver;
import com.botter.shop.canal.rabbitmq.MqSender;
import com.botter.shop.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-28 19:40
 * @Description 描述信息
 */
@RestController
@RequestMapping("/canal")
@Tag(name = "canal 服务")
public class TestMqController {

    @Autowired
    MqReceiver sender;

    @GetMapping("/mq/direct")
    public Result<String> mq() {
        sender.send("direct exchange");
        return Result.success("success");
    }

    @GetMapping("/mq/header")
    public Result<String> header() {
        sender.sendHeader("header exchange");
        return Result.success("success");
    }

    @GetMapping("/mq/fanout")
    public Result<String> fanout() {
        sender.sendFanout("fanout exchange");
        return Result.success("success");
    }

    @GetMapping("/mq/topic")
    public Result<String> topic() {
        sender.sendTopic("topic exchange");
        return Result.success("success");
    }
    
}
