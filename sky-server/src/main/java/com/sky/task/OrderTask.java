package com.sky.task;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 定时任务类，定时处理订单状态
 * OrderTask
 */
@Component 
@Slf4j 
public class OrderTask {
    
    @Autowired 
    private OrderMapper orderMapper;

    /**
     * 取消超时未付款订单
     */
    @Scheduled(cron = "0 * * * * ?") //每分钟触发一次
    public void processTimeoutOrder(){

        log.info("定时处理超时订单: {}", LocalDateTime.now());

        LocalDateTime pendingPaymentCutoff = LocalDateTime.now().plusMinutes(-15);

        //select * from orders where status = ? and order_time < (当前时间 - 15分钟)
        List<Orders> pendingPaymentOrders = orderMapper.getByStatusAndOrderTimeLT(
                Orders.PENDING_PAYMENT, pendingPaymentCutoff);

        if(pendingPaymentOrders != null && pendingPaymentOrders.size() > 0){
            for(Orders currentOrder : pendingPaymentOrders){
                Orders cancelledOrder = Orders.builder()
                        .id(currentOrder.getId())
                        .status(Orders.CANCELLED)
                        .payStatus(Orders.UN_PAID)
                        .cancelReason("订单超时，自动取消")
                        .cancelTime(LocalDateTime.now())
                        .build();
                int updatedRows = orderMapper.updateStatusIfCurrent(currentOrder.getId(),
                        Orders.PENDING_PAYMENT, Orders.UN_PAID, pendingPaymentCutoff, cancelledOrder);
                if (updatedRows != 1) {
                    log.debug("超时订单状态已变化，跳过自动取消，订单ID：{}", currentOrder.getId());
                }
            }
        }
    }

    /**
     * 完成超时配送订单
     */
    @Scheduled(cron = "0 0 1 * * ?") //每天凌晨1点触发一次
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单: {}", LocalDateTime.now());

        LocalDateTime deliveryCutoff = LocalDateTime.now().plusMinutes(-60);

        List<Orders> deliveryOrders = orderMapper.getByStatusAndOrderTimeLT(
                Orders.DELIVERY_IN_PROGRESS, deliveryCutoff);
        
        if(deliveryOrders != null && deliveryOrders.size() > 0){
            for(Orders currentOrder : deliveryOrders){
                Orders completedOrder = Orders.builder()
                        .id(currentOrder.getId())
                        .status(Orders.COMPLETED)
                        .build();
                int updatedRows = orderMapper.updateStatusIfCurrent(currentOrder.getId(),
                        Orders.DELIVERY_IN_PROGRESS, Orders.PAID, deliveryCutoff, completedOrder);
                if (updatedRows != 1) {
                    log.debug("配送订单状态已变化，跳过自动完成，订单ID：{}", currentOrder.getId());
                }
            }
        }
    }

}
