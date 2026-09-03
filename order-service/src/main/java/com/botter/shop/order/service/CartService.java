package com.botter.shop.order.service;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.common.utils.IdWorker;
import com.botter.shop.goods.api.GoodsApi;
import com.botter.shop.goods.dto.GoodsDTO;
import com.botter.shop.goods.model.Goods;
import com.botter.shop.order.model.CartItem;
import com.botter.shop.order.repository.CartItemRepository;
import com.botter.shop.order.vo.CartItemVO;
import com.botter.shop.order.vo.CartSizeVO;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-09-01 16:29
 * @Description 描述信息
 */
@Service
public class CartService {
    @Resource
    private CartItemRepository cartItemDao;

    @Resource
    private GoodsApi goodsApi;

    @Resource
    private IdWorker idWorker;

    public void incr(long userId, long goodsId) {
        CartItem ctDB = cartItemDao.findByUserIdAndGoodsId(userId , goodsId);
        if (ctDB == null) {
            setCount(userId, goodsId, 1);
            return;
        }
        setCount(userId, goodsId, ctDB.getCount() + 1);
    }

    public void decr(long userId, long goodsId) {
        CartItem ctDB = cartItemDao.findByUserIdAndGoodsId(userId , goodsId);
        if (ctDB == null) {
            return;
        }
        setCount(userId, goodsId, ctDB.getCount() - 1);
    }

    public void setCount(long userId, long goodsId, long count) {
        // 数量归零：直接删除该条目并结束
        if (count <= 0) {
            cartItemDao.deleteByUserIdAndGoodsId(userId, goodsId);
            return;
        }
        CartItem cartItemDB = cartItemDao.findByUserIdAndGoodsId(userId, goodsId);
        if (cartItemDB == null) {
            Result<GoodsDTO> goodsResult = goodsApi.get(goodsId);
            GoodsDTO goods = (goodsResult == null) ? null : goodsResult.getData();
            if (goods == null) {
                throw new GlobalException(ResultMsgEnum.GOODS_NOT_EXIST.fillArgs());
            }
            Instant now = Instant.now();
            CartItem cartItem = new CartItem();
            // CartItem 的 @Id 无 @GeneratedValue，ID 必须手动赋值（与 Order/OrderItem 统一用雪花ID）
            cartItem.setId(idWorker.nextId());
            cartItem.setUserId(userId);
            cartItem.setGoodsId(goodsId);
            cartItem.setCount(count);
            cartItem.setPrice(goods.price());
            cartItem.setCreateTime(now);
            cartItem.setUpdateTime(now);
            cartItemDao.save(cartItem);
        } else {
            cartItemDB.setCount(count);
            cartItemDB.setUpdateTime(Instant.now());
            cartItemDao.save(cartItemDB);
        }
    }
    
    public List<CartItemVO> list(int userId) {
        CartItem cartItem = new CartItem();
        cartItem.setUserId(Long.valueOf(userId));
        List<CartItemVO> cartItemVOS = new ArrayList<>();
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();
        List<CartItem> cartItems = cartItemDao.findAll(Example.of(cartItem , matcher));
        for (CartItem ct: cartItems) {
            CartItemVO cartItemVO = new CartItemVO(ct);
            Result<GoodsDTO> goodsResult = goodsApi.get(ct.getGoodsId());
            GoodsDTO goods = goodsResult.getData();
            if (goods != null) {
                cartItemVO.setImage(goods.image());
                cartItemVO.setTitle(goods.name());
                cartItemVO.setPriceNow(goods.price());
                cartItemVO.setTotalPriceNow(goods.price() * ct.getCount());
            }
            cartItemVOS.add(cartItemVO);
        }
        return cartItemVOS;
    }

    /**
     * 统计当前用户购物车的规模，供前端角标同步使用。
     * kinds = 不同商品的种类数（与购物车列表的行数一致）
     * total = 所有商品的总件数（每行 count 之和）
     */
    public CartSizeVO countSize(long userId) {
        List<CartItem> cartItems = cartItemDao.findByUserId(userId);
        long total = 0L;
        for (CartItem ct : cartItems) {
            // count 理论上非空（列定义为 NOT NULL DEFAULT 1），这里做一次防御
            if (ct.getCount() != null) {
                total += ct.getCount();
            }
        }
        return new CartSizeVO(cartItems.size(), total);
    }
}
