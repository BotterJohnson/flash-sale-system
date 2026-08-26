package com.botter.shop.goods.controller;


import com.botter.shop.common.result.Result;
import com.botter.shop.goods.dto.BrandDTO;
import com.botter.shop.goods.model.Brand;
import com.botter.shop.goods.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "品牌模块")
@RestController
@RequestMapping("/brand")
public class BrandController {

    @Autowired
    BrandService brandService;

    @Operation(summary = "根据id获取品牌")
    @GetMapping("/{id}")
    public Result<Brand> getById(@PathVariable("id") long id) {
        return Result.success(brandService.getById(id));
    }

    @PostMapping("/add")
    public Result<Long> add(@RequestBody Brand brand) {
        return Result.success(brandService.add(brand));
    }

    @PostMapping("/update")
    public Result<String> update(@RequestBody Brand brand) {
        brandService.update(brand);
        return Result.success("success");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable("id")long id) {
        brandService.deleteById(id);
        return Result.success("success");
    }

    @PostMapping("/search")
    public Result<List<BrandDTO>> search(@RequestBody Brand brand) {
        return Result.success(brandService.searchList(brand));
    }

    @GetMapping("/list/{page}/{size}")
    public Result<Page<BrandDTO>> listWithPage(@PathVariable int page, @PathVariable int size) {
        return Result.success(brandService.listWithPage(page, size));
    }

    @PostMapping("/search/{page}/{size}")
    public Result<Page<BrandDTO>> searchWithPage(@RequestBody Brand brand, @PathVariable int page, @PathVariable int size) {
        return Result.success(brandService.searchWithPage(brand, page, size));
    }

    @GetMapping("/listByCategoryId")
    public Result<List<BrandDTO>> listByCategoryId(@RequestParam long categoryId) {
        return Result.success(brandService.listByCategoryId(categoryId));
    }
}
