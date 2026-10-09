package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.Orders;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.utils.WeChatPayUtil;
import com.sky.websocket.WebSocketServer;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRefundProfileTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderDetailMapper orderDetailMapper;
    @Mock private AddressBookMapper addressBookMapper;
    @Mock private ShoppingCartMapper shoppingCartMapper;
    @Mock private UserMapper userMapper;
    @Mock private WeChatPayUtil weChatPayUtil;
    @Mock private WebSocketServer webSocketServer;
    @Mock private Environment environment;

    @InjectMocks
    private OrderServiceImpl orderService;

    @AfterEach
    void clearCurrentUser() {
        BaseContext.removeCurrentId();
    }

    /**
     * 验证已付款待接单订单取消成功后生产 profile 使用真实退款客户端
     *
     * @throws Exception 订单取消或退款失败
     */
    @Test
    void devAndProdProfilesMustUseRealRefundPath() throws Exception {
        BaseContext.setCurrentId(42L);
        ReflectionTestUtils.setField(orderService, "environment",
                environmentWithProfiles("dev", "prod"));
        when(orderMapper.getByIdAndUserId(10L, 42L)).thenReturn(paidOrder());
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID),
                isNull(), any(Orders.class))).thenReturn(1);

        orderService.userCancelById(10L);

        verify(weChatPayUtil).refund(eq("order-10"), eq("order-10"),
                any(BigDecimal.class), any(BigDecimal.class));
    }

    /**
     * 验证已付款待接单订单取消成功后仅 dev profile 保持模拟退款
     *
     * @throws Exception 订单取消或退款失败
     */
    @Test
    void devOnlyProfileMustKeepMockRefundForLocalTesting() throws Exception {
        BaseContext.setCurrentId(42L);
        ReflectionTestUtils.setField(orderService, "environment",
                environmentWithProfiles("dev"));
        when(orderMapper.getByIdAndUserId(10L, 42L)).thenReturn(paidOrder());
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID),
                isNull(), any(Orders.class))).thenReturn(1);

        orderService.userCancelById(10L);

        verify(weChatPayUtil, never()).refund(any(), any(), any(), any());
    }

    private Orders paidOrder() {
        return Orders.builder()
                .id(10L)
                .number("order-10")
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .build();
    }

    private MockEnvironment environmentWithProfiles(String... profiles) {
        MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.setActiveProfiles(profiles);
        return mockEnvironment;
    }
}
