package com.botter.shop.order.service;

import com.botter.shop.common.exception.GlobalException;
import com.botter.shop.common.result.CodeMsg;
import com.botter.shop.common.result.Result;
import com.botter.shop.common.result.ResultMsgEnum;
import com.botter.shop.common.utils.IdWorker;
import com.botter.shop.goods.api.GoodsApi;
import com.botter.shop.goods.dto.GoodsDTO;
import com.botter.shop.order.model.Order;
import com.botter.shop.order.model.OrderItem;
import com.botter.shop.order.repository.CartItemRepository;
import com.botter.shop.order.repository.OrderItemRepository;
import com.botter.shop.order.repository.OrderRepository;
import com.botter.shop.order.vo.OrderVO;
import com.botter.shop.order.vo.OrdersParam;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-09-03 12:02
 * @Description 描述信息
 */
@Service
public class OrderService {
    
    @Resource 
    private OrderRepository orderDao;
    
    @Resource
    private OrderItemRepository orderItemDao;
    
    @Resource
    private CartItemRepository cartItemDao;
    
    @Resource
    private IdWorker idWorker;
    
    @Resource
    private GoodsApi goodsApi;
    

    public Order genOrder(int userId, OrdersParam ordersParam) {
        /*
           IDEA 生成Order需要的条件
           1. 要根据cartIds去查找对应的carts
           2. 计算总的金额
           3. 要记录订单的子项，也就是商品项
         */
        var order = new Order();
        order.setId(idWorker.nextId());
        order.setUserId(Long.valueOf(userId));
        order.setReceiveAddress(ordersParam.getReceiveAddress());
        order.setPayType((short) ordersParam.getPayType());
        order.setConsumerMsg(ordersParam.getConsumerMsg());
        List<OrderItem> addDbData = new ArrayList<>();
        // 计算总的订单金额
        Long totalPrice = 0l;
        for (long cartItemId : ordersParam.getCartItemIds()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setId(idWorker.nextId());
            orderItem.setOrderId(order.getId());
            orderItem.setCartItemId(cartItemId);
            var cartItem = cartItemDao.findById(cartItemId)
                    .orElseThrow(() ->new GlobalException(ResultMsgEnum.CART_ITEM_NOT_EXIST));
            orderItem.setGoodsId(cartItem.getGoodsId());
            orderItem.setUserId(cartItem.getUserId());
            orderItem.setCount(cartItem.getCount());
            var result = goodsApi.get(orderItem.getGoodsId());
            var data = result.getData();
            long actualPrice = data.price() * cartItem.getCount();
            orderItem.setGoodsImage(data.image());
            orderItem.setGoodsName(data.name());
            orderItem.setPrice(data.price());
            orderItem.setDiscountPrice(0L);
            orderItem.setActualPrice(actualPrice);
            
            //REVIEW 需要解决扣减库存，防止出现超卖的情况
            var decrStock = goodsApi.decrStock(cartItem.getGoodsId(), Math.toIntExact(cartItem.getCount()));
            if (decrStock.getCode() != ResultMsgEnum.SUCCESS.getCode()) {
                throw new GlobalException(ResultMsgEnum.GOODS_STOCK_SHORTAGE);
            }

//            addDbData.add(orderItem);
            orderItem.setCreateTime(new Date().toInstant() );
            orderItem.setUpdateTime(new Date().toInstant());
            orderItemDao.save(orderItem);
            totalPrice += actualPrice;
        }
//        orderItemDao.saveAll(addDbData);
        cartItemDao.deleteAllByIdInBatch(ordersParam.getCartItemIds());
        //TODO 用户积分还没有算
        order.setTotalPrice(totalPrice);
        order.setActualPrice(totalPrice);
        order.setDiscountPrice(0l);
        order.setUpdateTime(new Date().toInstant());
        order.setCreateTime(new Date().toInstant());
        order.setStatus(0);
        orderDao.save(order);
        return order;
    }
    
    
    public OrderVO getOrderById(Long orderId) {
        var order = orderDao.findById( orderId)
                .orElseThrow(() ->new GlobalException(ResultMsgEnum.ORDER_NOT_EXIST.fillArgs(orderId)));
        var orderVO = new OrderVO();
        // 拷贝订单全部字段（价格/地址/支付方式/状态等），原来只拷了id会导致订单信息栏全为空
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderItems(orderItemDao.findOrderItemByOrderId(orderId));
        return orderVO;
    }

    public List<OrderVO> listOrders(Long userId) {
        var orders = orderDao.findByUserId(userId);
        var result = new ArrayList<OrderVO>();
        for (var order : orders) {
            var vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            vo.setOrderItems(orderItemDao.findOrderItemByOrderId(order.getId()));
            result.add(vo);
        }
        return result;
    }
}
