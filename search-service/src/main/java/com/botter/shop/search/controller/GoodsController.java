package com.botter.shop.search.controller;

import com.botter.shop.common.result.Result;
import com.botter.shop.search.model.SearchGoodsParam;
import com.botter.shop.search.model.SearchGoodsRes;
import com.botter.shop.search.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-25 22:04
 * @Description 描述信息
 */
@Slf4j
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


    @Operation(summary = "在es中搜索")
    @PostMapping("/search/{page}/{size}")
    public Result<SearchGoodsRes> search(@PathVariable int page, @PathVariable int size,
                                         @RequestBody SearchGoodsParam param
    ){
        if (param == null){
            param = new SearchGoodsParam();
        }
        log.info(param.toString());
        return Result.success(goodsService.search(param, page, size));
    }
}
