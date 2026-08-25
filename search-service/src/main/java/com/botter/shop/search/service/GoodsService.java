package com.botter.shop.search.service;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.goods.api.GoodsApi;
import com.botter.shop.goods.dto.GoodsDTO;
import com.botter.shop.goods.model.Goods;
import com.botter.shop.search.model.GoodsEsInfo;
import com.botter.shop.search.repository.GoodsEsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-25 22:01
 * @Description 描述信息
 */
@Service
public class GoodsService {
    @Autowired
    private GoodsApi goodsApi;

    @Autowired
    private GoodsEsRepository goodsEsDao;


    public void ExportMysqlToEs(){
        try{
            Result<List<GoodsDTO>> allValidGoods = goodsApi.getAllValidGoods();
            List<GoodsEsInfo> goodsEsInfoList = new ArrayList<>();
            for (GoodsDTO validgood : allValidGoods.getData()){
                goodsEsInfoList.add(new GoodsEsInfo());
            }
            goodsEsDao.saveAll(goodsEsInfoList);
        }catch (Exception e){
            throw new GlobalException(ResultMsgEnum.ES_SERVICE_ERROR);
        }

    }
}
