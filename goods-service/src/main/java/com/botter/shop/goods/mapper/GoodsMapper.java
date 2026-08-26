package com.botter.shop.goods.mapper;

import com.botter.shop.goods.dto.GoodsDTO;
import com.botter.shop.goods.model.Goods;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-25 22:26
 * @Description 描述信息
 */
@Mapper(componentModel = "spring")
public interface GoodsMapper {

    String ZongId = "Asia/Shanghai";

    GoodsDTO toGoodsDTO(Goods goods);

    Goods toGoods(GoodsDTO goodsDTO);

    List<GoodsDTO> toGoodsDTOList(List<Goods> goods);

    List<Goods>  toGoodsList(List<GoodsDTO> goodsDTOList);


    default Date map(Instant value) {
        return value == null ? null : Date.from(value);
    }

    default Instant map(Date value) {
        return value == null ? null : value.toInstant();
    }
}
