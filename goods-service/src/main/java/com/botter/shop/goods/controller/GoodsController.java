package com.botter.shop.goods.controller;


import com.botter.shop.common.result.Result;
import com.botter.shop.goods.dto.GoodsDTO;

import com.botter.shop.goods.service.GoodsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "商品模块")
@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    GoodsService goodsService;

//    @Operation(summary = "根据基础商品名称, 类目， 品牌 生成模拟商品数据")
//    @GetMapping("/genMockGoods")
//    public Result<List<Goods>> genMockGoods(@RequestParam String baseName,
//                                            @RequestParam long categoryId,
//                                            @RequestParam long brandId,
//                                            @RequestParam long minPrice,
//                                            @RequestParam long maxPrice,
//                                            @RequestParam long stock) {
//        return Result.success(goodsService.genMockGoodsByName(baseName, categoryId, brandId, stock, minPrice, maxPrice));
//    }

    @GetMapping("/getAllValidGoods")
    public Result<List<GoodsDTO>> getAllValidGoods() {
        return Result.success(goodsService.getByStatus(0));
    }

    @GetMapping("/get/{id}")
    public Result<GoodsDTO> get(@PathVariable(name = "id") Long id) {
        return Result.success(goodsService.getGoodsById(id));
    }

    @GetMapping("/decrStock")
    Result<String> decrStock(@RequestParam Long goodsId, @RequestParam Integer count) {
        goodsService.decrStock(goodsId, count);
        return Result.success("success");
    }
}
