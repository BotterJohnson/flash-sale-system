package com.botter.shop.search.controller;

import com.botter.shop.common.result.Result;
import com.botter.shop.search.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-25 22:04
 * @Description 描述信息
 */
@RestController
@RequestMapping("/search/goods")
@CrossOrigin
@Tag(name = "商品搜索服务")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Operation(summary = "将数据库中的商品信息导入到es中")
    @GetMapping("/exportData2ES")
    public Result<String> exportData2ES(){
        goodsService.ExportMysqlToEs();
        return Result.success("OK");
    }
}
