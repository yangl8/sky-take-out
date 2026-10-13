package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/order")
@Api(tags = "Mock payment endpoints")
@Profile("dev & !prod")
@ConditionalOnProperty(prefix = "sky.payment", name = "mock-enabled", havingValue = "true")
public class MockPaymentController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/mockPaySuccess/{orderNumber}/{orderId}")
    @ApiOperation("Simulate successful payment")
    public Result mockPaySuccess(@PathVariable String orderNumber, @PathVariable Long orderId) {
        orderService.mockPaySuccessForUser(orderNumber, orderId);
        return Result.success();
    }
}
