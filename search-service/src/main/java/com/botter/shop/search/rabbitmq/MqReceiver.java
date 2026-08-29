package com.botter.shop.search.rabbitmq;

import com.botter.shop.common.rabbitmq.CanalMessage;
import com.botter.shop.common.rabbitmq.OperationType;
import com.botter.shop.common.utils.BeanUtils;
import com.botter.shop.goods.api.GoodsApi;
import com.botter.shop.search.model.GoodsEsInfo;
import com.botter.shop.search.repository.GoodsEsRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * @ProjectName botter-shop-mic
 * @Author Botter
 * @Create 2026-08-29 23:08
 * @Description 描述信息
 */
@Service
public class MqReceiver {
    private static final Logger log = LoggerFactory.getLogger(MqReceiver.class);
    @Resource
    private AmqpTemplate amqpTemplate;
    
    @Resource
    private GoodsEsRepository goodsEsRepository;
    
    @Resource 
    private GoodsApi goodsApi;
    
    @RabbitListener(queues = MQConfig.CANAL_QUEUE)
    public void receiverCanalMsg(String msg){
        log.info("receive canal message:" + msg);
        var canalMessage = BeanUtils.stringToBean(msg, CanalMessage.class);
        
        if (canalMessage.getOperationType() == OperationType.DELETE) {
            goodsEsRepository.deleteById(canalMessage.getPrimaryKey());
        } else {
            var goodsEsInfo = new GoodsEsInfo(goodsApi.get(canalMessage.getPrimaryKey()).getData());
            log.info(goodsEsInfo.toString());
            goodsEsRepository.save(goodsEsInfo);
        }
    }

}
