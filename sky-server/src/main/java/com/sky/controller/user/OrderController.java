package com.sky.controller.user;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "Customer order endpoints")
@Slf4j 
public class OrderController {
    
    @Autowired 
    private OrderService orderService;

    /**
     * Returns the current time plus 30 minutes as the estimated delivery time
     *
     * @return Estimated delivery time based on the current server time
     */
    @GetMapping("/getEstimatedDeliveryTime")
    @ApiOperation("Get estimated delivery time")
    public Result<LocalDateTime> getEstimatedDeliveryTime() {
        return Result.success(LocalDateTime.now().plusMinutes(30));
    }

    @PostMapping("/submit")
    @ApiOperation("Place order")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("Order submission: {}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    @PutMapping("/payment")
    @ApiOperation("Pay for order")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("Order payment request: {}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("Created prepayment transaction: {}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * Queries the current user's order history
     * @param page Page number
     * @param pageSize Number of orders per page
     * @param status Order status: 1 pending payment, 2 awaiting acceptance, 3 accepted, 4 in delivery, 5 completed, 6 cancelled
     * @param payStatus Optional payment status filter
     * @return The requested page of the current user's orders
     */
    @GetMapping("/historyOrders")
    @ApiOperation("Order history")
    public Result<PageResult> page(int page, int pageSize, Integer status, Integer payStatus) {
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status, payStatus);
        return Result.success(pageResult);
    }

    @GetMapping("/orderDetail/{id}")
    @ApiOperation("Get order details")
    public Result<OrderVO> details(@PathVariable("id") Long id) {
        OrderVO orderVO = orderService.detailsForUser(id);
        return Result.success(orderVO);
    }

    @PutMapping("/cancel/{id}")
    @ApiOperation("Cancel order")
    public Result cancel(@PathVariable("id") Long id) throws Exception {
        orderService.userCancelById(id);
        return Result.success();
    }

    @PostMapping("/repetition/{id}")
    @ApiOperation("Reorder")
    public Result repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    @GetMapping("/reminder/{id}")
    @ApiOperation("Customer order reminder")
    public Result reminder(@PathVariable("id") Long id){
        orderService.reminder(id);
        return Result.success();
    }
}
