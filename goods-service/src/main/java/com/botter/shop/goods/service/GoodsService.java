package com.botter.shop.goods.service;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.goods.dto.GoodsDTO;
import com.botter.shop.goods.mapper.GoodsMapper;
import com.botter.shop.goods.model.Goods;
import com.botter.shop.goods.repository.GoodsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-25 22:20
 * @Description 描述信息
 */
@Service
public class GoodsService {
    private static final Logger log = LoggerFactory.getLogger(GoodsService.class);
    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private GoodsMapper goodsMapper;

    public List<GoodsDTO> getByStatus(int status) {
        var byStatus = goodsRepository.getByStatus(status);
        return goodsMapper.toGoodsDTOList(byStatus);
    }

    public GoodsDTO getGoodsById(Long id) {
        Goods search = goodsRepository.findById(id).orElseThrow(
                () -> new GlobalException(ResultMsgEnum.DATA_NOT_EXIST)
        );
        return goodsMapper.toGoodsDTO(search);
    }

    public void decrStock(Long goodsId, Integer count) {
        if (goodsRepository.decrStock(goodsId, count) <= 0) {
            throw new GlobalException(ResultMsgEnum.GOODS_STOCK_SHORTAGE);
        }
        log.info("decrStock goodsId={}, count={} , success", goodsId, count);
    }
}
