package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/order")
@Api(tags = "模拟支付接口")
@Profile("dev")
public class MockPaymentController {

    @Autowired
    private OrderService orderService;

    /**
     * 模拟支付成功
     *
     * @param orderNumber 订单号
     * @return
     */
    @PostMapping("/mockPaySuccess/{orderNumber}")
    @ApiOperation("模拟支付成功")
    public Result mockPaySuccess(@PathVariable String orderNumber) {
        orderService.paySuccess(orderNumber);
        return Result.success();
    }
}
