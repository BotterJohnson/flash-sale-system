package com.botter.shop.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;

import com.alibaba.fastjson.JSONObject;
import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.goods.api.GoodsApi;
import com.botter.shop.goods.dto.GoodsDTO;
import com.botter.shop.search.model.GoodsEsInfo;
import com.botter.shop.search.model.SearchGoodsParam;
import com.botter.shop.search.model.SearchGoodsRes;
import com.botter.shop.search.repository.GoodsEsRepository;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import java.util.*;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;

import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.util.StringUtils;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-25 22:01
 * @Description 描述信息
 */
@Slf4j
@Service
public class GoodsService {
    private static Logger logger = LoggerFactory.getLogger(GoodsService.class);
    @Autowired
    private GoodsApi goodsApi;

    @Autowired
    private GoodsEsRepository goodsEsDao;


    @Autowired
    private ElasticsearchOperations elasticsearchRestTemplate;

    public void ExportMysqlToEs(){

        try{
            var indexOps = elasticsearchRestTemplate.indexOps(GoodsEsInfo.class);
            if (indexOps.exists()) {
                indexOps.delete();
            }
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping(GoodsEsInfo.class));

            Result<List<GoodsDTO>> allValidGoods = goodsApi.getAllValidGoods();
            if (allValidGoods == null || allValidGoods.getCode() != 200
                    || allValidGoods.getData() == null) {
                throw new GlobalException(ResultMsgEnum.ES_SERVICE_ERROR);
            }

            List<GoodsEsInfo> goodsEsInfoList = new ArrayList<>();
            for (GoodsDTO validGood : allValidGoods.getData()){
                goodsEsInfoList.add(new GoodsEsInfo(validGood));
            }
            goodsEsDao.saveAll(goodsEsInfoList);
            log.info("export success for {} data", goodsEsInfoList.size());
        }catch (Exception e){
            if (e instanceof GlobalException globalException) {
                throw globalException;
            }
            throw new GlobalException(ResultMsgEnum.ES_SERVICE_ERROR.fillArgs(e.getMessage()));
        }

    }

    /***
     *
     * @param params 查询条件map
     * {
     *     "key": "关键词", // 关键词查询， 搜索 商品名称， 类目， 品牌
     *     "category": "类目名称",
     *     "brand": "品牌名称",
     *     "specs_规格name": "规格名称",
     *     "min_price": "0", // 0
     *     "max_price": "100", // <=0 表示没有约束
     *     "orderField": "排序字段",
     *     "orderType": "排序方式" // asc, desc
     * }
     * @return
     */
    public SearchGoodsRes search(SearchGoodsParam params, int page, int size) {
        SearchGoodsRes res = new SearchGoodsRes();
        //category聚合
        Aggregation categoryNameAgg = Aggregation.of(a -> a.terms(t ->
                       t.field("categoryName.keyword")
                        .size(100)));
        //平牌聚合
        Aggregation brandNameAgg = Aggregation.of(a -> a.terms(t -> t.field("brandName").size(100)));

        //规格聚合
        Aggregation specAgg =  Aggregation.of(a -> a.terms(t -> t.field("specsJson").size(10000)));

        //查询builder
        var query = NativeQuery.builder()
                .withAggregation("goodsCategoryName", categoryNameAgg)
                .withAggregation("goodsBrandName", brandNameAgg)
                .withAggregation("goodsSpecJson", specAgg);


        var boolQueryBuilder = new BoolQuery.Builder();

        // 2. 关键词搜索（must + should）
        if (params.getKey() != null && !params.getKey().isEmpty()) {
            // 分词全文搜索：name 权重最高，categoryName 其次
            boolQueryBuilder.must(QueryBuilders.multiMatch()
                            .query(params.getKey())
                            .fields("name^3", "categoryName^1")
                            .build()._toQuery());
            // fullName 同时包含两个词时大幅提权（如"华为手机"既有品牌又有类目）
            boolQueryBuilder.should(QueryBuilders.match()
                    .field("fullName")
                    .query(params.getKey())
                    .operator(Operator.And)
                    .boost(3.0f)
                    .build()._toQuery());
            // brandName 精确匹配提权（keyword 字段用 term 查询）
            boolQueryBuilder.should(QueryBuilders.term()
                    .field("brandName")
                    .value(params.getKey())
                    .boost(5.0f)
                    .build()._toQuery());
        }
        //TODO 点击一个类目， 品牌， 规格之后就不需要再显示相应的列表了
        // 分类过滤
        if (StringUtils.hasText(params.getCategory()) && !"null".equalsIgnoreCase(params.getCategory())) {
            res.setCategory(params.getCategory());
            boolQueryBuilder.filter(f->f.term(t -> t.field("categoryName.keyword")
                    .value(params.getCategory())));
        }

        // 品牌过滤
        if (StringUtils.hasText(params.getBrand()) && !"null".equalsIgnoreCase(params.getBrand())) {
            res.setBrand(params.getBrand());
            boolQueryBuilder.filter(f->f.term(t ->
                    t.field("brandName").value(params.getBrand())));
        }

        // 规格过滤
        if (params.getSpecsValueMap() != null && !params.getSpecsValueMap().isEmpty()) {
            res.setSpecsValueMap(params.getSpecsValueMap());
            for (Map.Entry<String, String> entry : params.getSpecsValueMap().entrySet() ) {
                boolQueryBuilder.filter(f->f.term(t ->
                        t.field("specsMap."+entry.getKey()+".keyword").value(entry.getValue())));
            }
        }

        //价格范围过滤
        if (params.getMaxPrice() > 0){
            RangeQuery priceRangequery = RangeQuery.of(ra -> ra.term(t -> t.field("price")
                    .gte(String.valueOf((params.getMinPrice())))
                    .lte(String.valueOf(params.getMaxPrice()))
            ));
            boolQueryBuilder.must(priceRangequery);
        }
        query.withQuery(boolQueryBuilder.build()._toQuery());
        // 分页查询
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        query.withPageable(pageRequest);
        // 排序
        if (StringUtils.hasText(params.getOrderField()) && StringUtils.hasText(params.getOrderType())) {
            // 安全转换排序方向（支持 "asc"/"desc"/"ASC"/"DESC"）
            Sort.Direction direction = Sort.Direction.fromString(params.getOrderType());
            query.withSort(Sort.by(direction, params.getOrderField()));
        }


        //高亮显示
        // 1. 构建 HighlightField（注意：是 Spring Data 的，不是 ES 客户端的）
        List<HighlightField> highlightFields = new ArrayList<>();
        highlightFields.add(new HighlightField("name", HighlightFieldParameters.builder()
                .withPreTags("<span style='color:#FF0000;background-color:#FFFF00;font-weight:bold;font-size:20px;'>")
                .withPostTags("</span>")
                .withNumberOfFragments(0)
                .build()));
        highlightFields.add(new HighlightField("categoryName" , HighlightFieldParameters.builder()
                .withPreTags("<span style='color:#0066FF;background-color:#FFB6C1;font-style:italic;text-decoration:underline;'>")
                .withPostTags("</span>")
                .withNumberOfFragments(0)
                .build()));
        var highlight = new Highlight(highlightFields);

        query.withHighlightQuery(new HighlightQuery(highlight,null));
        //查询
        var goodsEsInfoSearchHits = elasticsearchRestTemplate.search(query.build(), GoodsEsInfo.class);
        //构造返回结果

        List<String> categoryNames = null;
        var searchHits = goodsEsInfoSearchHits.getSearchHits();

        ElasticsearchAggregations esAggs = (ElasticsearchAggregations) goodsEsInfoSearchHits.getAggregations();
        Map<String, Aggregate> aggsResultMap = new HashMap<>();
        if (esAggs != null && esAggs.aggregations() != null) {
            for (org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation agg : esAggs.aggregations()) {
                aggsResultMap.put(agg.aggregation().getName(), agg.aggregation().getAggregate());
            }
        }

        if (StringUtils.isEmpty(params.getCategory())) { 
            categoryNames = new ArrayList<>();
            Aggregate catAgg = aggsResultMap.get("goodsCategoryName");
            if (catAgg != null && catAgg.isSterms()) {
                for (StringTermsBucket bucket : catAgg.sterms().buckets().array()) {
                    categoryNames.add(bucket.key().stringValue());
                }
            }
        }

        List<String> brandNames = null;
        if (StringUtils.isEmpty(params.getBrand())) {
            brandNames = new ArrayList<>();
            Aggregate brandAgg = aggsResultMap.get("goodsBrandName");
            if (brandAgg != null && brandAgg.isSterms()) {
                for (StringTermsBucket bucket : brandAgg.sterms().buckets().array()) {
                    brandNames.add(bucket.key().stringValue());
                }
            }
        }

        Map<String, Set<String>> specsRes = new LinkedHashMap<>();
        Aggregate resSpecAgg = aggsResultMap.get("goodsSpecJson");
        if (resSpecAgg != null && resSpecAgg.isSterms()) {
            for (StringTermsBucket bucket : resSpecAgg.sterms().buckets().array()) {
                String jsonStr = bucket.key().stringValue();
                if (!StringUtils.hasText(jsonStr)) continue;
                try {
                    JSONObject specsJson = JSONObject.parseObject(jsonStr);
                    for (Map.Entry<String, Object> specsEntry : specsJson.entrySet()) {
                        if (params.getSpecsValueMap() != null && params.getSpecsValueMap().containsKey(specsEntry.getKey())) {
                            continue; 
                        }
                        specsRes.computeIfAbsent(specsEntry.getKey(), k -> new LinkedHashSet<>())
                                .add(String.valueOf(specsEntry.getValue()));
                    }
                } catch (Exception ignored) {}
            }
        }

        List<GoodsEsInfo> rows = new ArrayList<>();

        for (SearchHit<GoodsEsInfo> searchHit : goodsEsInfoSearchHits) {
            GoodsEsInfo goodsEsInfo = searchHit.getContent();
            // 获取高亮结果
            List<String> nameList = searchHit.getHighlightField("name");
            if (!nameList.isEmpty()) {
                String nameHighlight = nameList.getFirst();
                goodsEsInfo.setName(nameHighlight);
            }
            List<String> categoryNameList = searchHit.getHighlightField("categoryName");
            if (!categoryNameList.isEmpty()) {
                goodsEsInfo.setCategoryName(categoryNameList.getFirst());
            }
            rows.add(goodsEsInfo);
        }
        if (!searchHits.isEmpty()) {
            logger.info(searchHits.getFirst().getContent().toString());
        }
        long totalSize = goodsEsInfoSearchHits.getTotalHits();
        long pages = totalSize / size + (totalSize % size > 0 ? 1 : 0);
        res.setPage(page);
        res.setTotal(totalSize);
        res.setTotalPages(pages);
        res.setGoodsEsInfoList(rows);
        res.setCategoryList(categoryNames);
        res.setBrandList(brandNames);
        res.setSpecsList(specsRes);
        return res;
    }
}
