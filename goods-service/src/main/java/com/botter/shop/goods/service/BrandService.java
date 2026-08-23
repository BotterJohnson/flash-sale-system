package com.botter.shop.goods.service;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.CodeMsg;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.goods.model.Brand;
import com.botter.shop.goods.repository.BrandRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-23 20:58
 * @Description 描述信息
 */
@Service
public class BrandService {
    private static Logger logger = LoggerFactory.getLogger(BrandService.class);
    @Resource
    private BrandRepository brandRepository;

    public Brand getById(long id) {
        return brandRepository.getReferenceById(id);
    }


    public Long add(Brand brand) {
        try {
            brandRepository.save(brand);
            return brand.getId();
        }catch (Exception e) {
            throw new GlobalException(ResultMsgEnum.DATA_NOT_EXIST);
        }
    }


    public void update(Brand brand) {
        this.add(brand);
    }


    public void deleteById(long id) {
        brandRepository.deleteById(id);
    }


    public List<Brand> searchList(Brand brand) {
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        Example<Brand> example = Example.of(brand, exampleMatcher);
        return brandRepository.findAll(example);
    }
}
