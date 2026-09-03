package com.botter.shop.search.repository;

import com.botter.shop.search.model.GoodsEsInfo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-25 21:57
 * @Description 描述信息
 */
public interface GoodsEsRepository extends ElasticsearchRepository<GoodsEsInfo, String> {
}
