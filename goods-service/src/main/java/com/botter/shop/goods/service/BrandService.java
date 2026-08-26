package com.botter.shop.goods.service;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.goods.dto.BrandDTO;
import com.botter.shop.goods.mapper.BrandMapper;
import com.botter.shop.goods.model.Brand;
import com.botter.shop.goods.model.CategoryBrand;
import com.botter.shop.goods.repository.BrandRepository;
import com.botter.shop.goods.repository.CategoryBrandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
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
    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private CategoryBrandRepository categoryBrandRepository;

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


    public List<BrandDTO> searchList(Brand brand) {
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        Example<Brand> example = Example.of(brand, exampleMatcher);
        var res = brandRepository.findAll(example);
        return brandMapper.toDTOList(res);
    }

    public Page<BrandDTO> listWithPage(int page, int size) {
        var brands = brandRepository.findAll(PageRequest.of(page, size));
        return brandMapper.toDTOPage(brands);
    }

    public Page<BrandDTO> searchWithPage(Brand brand, int page, int size) {
        var brands = brandRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return brandMapper.toDTOPage(brands);
    }

    public List<BrandDTO> listByCategoryId(long categoryId) {
        List<CategoryBrand> categoryBrands = categoryBrandRepository.findByCategoryId(categoryId);
        List<Long> brandIDs = categoryBrands.stream().map(CategoryBrand::getBrandId).toList();
        var allById = brandRepository.findAllById(brandIDs);
        return brandMapper.toDTOList(allById);
    }
}
