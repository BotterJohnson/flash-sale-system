package com.botter.shop.seckill.controller;

import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.seckill.auth.TokenUserResolver;
import com.botter.shop.seckill.dto.SeckillGoodsVO;
import com.botter.shop.seckill.dto.SeckillOrderVO;
import com.botter.shop.seckill.dto.SeckillUser;
import com.botter.shop.seckill.service.PublishService;
import com.botter.shop.seckill.service.SeckillService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀接口：
 * <ul>
 *   <li>{@code GET /seckill/list} —— 秒杀活动列表</li>
 *   <li>{@code POST /seckill/do?goodsId=} —— 执行秒杀，返回订单号</li>
 *   <li>{@code POST /seckill/publishService} —— 发布秒杀活动（演示用）</li>
 *   <li>{@code POST /seckill/reset?goodsId=} —— 重置活动（演示用）</li>
 *   <li>{@code GET /seckill/myOrders} —— 我的秒杀订单</li>
 * </ul>
 *
 * <p>后端 id 一律用 {@code Long}（19 位雪花 < Long.MAX，能装下不丢精度）。
 * 真正的精度坑在前端 JS：Number 仅 16 位精度，<b>前端必须按字符串传递</b>。
 *
 * <p>网关对 /seckill/** 不做 StripPrefix，路径原样转发。
 */
@Tag(name = "秒杀模块")
@RestController
@RequestMapping("/seckill")
public class SeckillController {
    private static final Logger log = LoggerFactory.getLogger(SeckillController.class);

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private TokenUserResolver tokenUserResolver;
    
    @Autowired
    private PublishService publishService;

    @GetMapping("/list")
    public Result<List<SeckillGoodsVO>> list(HttpServletRequest request) {
        try {
            // 游客可浏览：解析不到用户就传 null，登录后才会带上「已抢到」标记
            SeckillUser user = tokenUserResolver.resolveOrNull(request);
            return Result.success(seckillService.list(user));
        } catch (Exception e) {
            log.error("秒杀商品列表查询异常", e);
            return Result.error(ResultMsgEnum.SERVER_ERROR);
        }
    }

    @PostMapping("/do")
    public Result<String> doSeckill(@RequestParam Long goodsId, HttpServletRequest request) {
        // 必须登录：token 缺失或 Redis 里查不到直接抛 TOKEN_EMPTY / SESSION_ERROR
//        SeckillUser user = tokenUserResolver.resolve(request);
//        return Result.success(seckillService.doSeckillByRedisLua(goodsId, user));

        var satoken = request.getHeader("satoken");
        if (satoken == null || satoken.isBlank()){
            return Result.error(ResultMsgEnum.TOKEN_EMPTY);
        }
        return Result.success(seckillService.doSeckillByRedisLua(goodsId, satoken));

    }

    @PostMapping("/publishService")
    public Result<SeckillGoodsVO> publish(@RequestParam Long goodsId,
                                          @RequestParam Long seckillPrice,
                                          @RequestParam Integer stock,
                                          @RequestParam(required = false) Integer delayMinutes,
                                          @RequestParam(required = false) Integer durationMinutes) {
        return Result.success(publishService.publish(goodsId, seckillPrice, stock, delayMinutes, durationMinutes));
    }

    @PostMapping("/reset")
    public Result<SeckillGoodsVO> reset(@RequestParam Long goodsId,
                                        @RequestParam(required = false) Integer durationMinutes) {
        return Result.success(seckillService.reset(goodsId, durationMinutes));
    }

    @GetMapping("/myOrders")
    public Result<List<SeckillOrderVO>> myOrders(HttpServletRequest request) {
        SeckillUser user = tokenUserResolver.resolve(request);
        return Result.success(seckillService.myOrders(user));
    }
}
