package com.sky.task;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;

import lombok.extern.slf4j.Slf4j;

@Component 
@Slf4j 
public class OrderTask {
    
    @Autowired 
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){

        log.info("Processing expired orders: {}", LocalDateTime.now());

        LocalDateTime pendingPaymentCutoff = LocalDateTime.now().plusMinutes(-15);

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
                    log.debug("Expired order state changed; skipping automatic cancellation for order {}", currentOrder.getId());
                }
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("Processing orders in delivery: {}", LocalDateTime.now());

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
                    log.debug("Delivery order state changed; skipping automatic completion for order {}", currentOrder.getId());
                }
            }
        }
    }

}
