package com.botter.shop.goods.api;


import com.botter.shop.common.result.Result;
import com.botter.shop.goods.dto.GoodsDTO;
import com.botter.shop.goods.model.Goods;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "goods", path = "/goods")
public interface GoodsApi {
    @GetMapping("/getAllValidGoods")
    Result<List<GoodsDTO>> getAllValidGoods();

    @GetMapping("/get/{id}")
    Result<GoodsDTO> get(@PathVariable(name = "id") Long id);

    @GetMapping("/decrStock")
    Result<String> decrStock(@RequestParam Long goodsId, @RequestParam Integer count);
}
