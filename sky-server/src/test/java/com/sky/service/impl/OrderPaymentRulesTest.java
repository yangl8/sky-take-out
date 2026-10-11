package com.sky.service.impl;

import com.sky.config.DeliveryTimeProperties;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.DeliveryTimeValidationService;
import com.sky.utils.WeChatPayUtil;
import com.sky.websocket.WebSocketServer;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

/**
 * 验证订单支付期限、配送条件及首次支付时间更新
 */
@ExtendWith(MockitoExtension.class)
class OrderPaymentRulesTest {

    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");

    @Mock private OrderMapper orderMapper;
    @Mock private OrderDetailMapper orderDetailMapper;
    @Mock private AddressBookMapper addressBookMapper;
    @Mock private DishMapper dishMapper;
    @Mock private SetmealMapper setmealMapper;
    @Mock private ShoppingCartMapper shoppingCartMapper;
    @Mock private UserMapper userMapper;
    @Mock private WeChatPayUtil weChatPayUtil;
    @Mock private WebSocketServer webSocketServer;
    @Mock private Environment environment;
    @Mock private RedisTemplate redisTemplate;
    @Mock private ValueOperations valueOperations;

    @InjectMocks
    private OrderServiceImpl orderService;

    private DeliveryTimeProperties properties;
    private Clock clock;

    @BeforeEach
    void setUp() {
        properties = new DeliveryTimeProperties();
        properties.validate();
        clock = Clock.fixed(NOW, ZoneId.of("UTC"));
        ReflectionTestUtils.setField(orderService, "deliveryTimeProperties", properties);
        ReflectionTestUtils.setField(orderService, "clock", clock);
        ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService",
                new DeliveryTimeValidationService(properties, clock));
        ReflectionTestUtils.setField(orderService, "redisTemplate", redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void clearUser() {
        BaseContext.removeCurrentId();
    }

    @Test
    void immediatePaymentShouldBeAllowedWhenShopIsOpenDuringBusinessHours() throws Exception {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByNumberAndUserId("order-1", 1L)).thenReturn(order(Orders.PENDING_PAYMENT,
                Orders.UN_PAID, 1, LocalDateTime.of(2026, 10, 11, 17, 55), null));
        when(valueOperations.get("SHOP STATUS")).thenReturn(1);
        when(userMapper.getById(1L)).thenReturn(new User());
        when(weChatPayUtil.pay(eq("order-1"), any(BigDecimal.class), any(), any())).thenReturn(new com.alibaba.fastjson.JSONObject());

        orderService.payment(request("order-1"));

        verify(weChatPayUtil).pay(eq("order-1"), any(BigDecimal.class), any(), any());
    }

    @Test
    void immediatePaymentShouldRejectClosedOrMissingRedisStatus() throws Exception {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByNumberAndUserId("order-1", 1L)).thenReturn(order(Orders.PENDING_PAYMENT,
                Orders.UN_PAID, 1, LocalDateTime.of(2026, 10, 11, 17, 55), null));
        when(valueOperations.get("SHOP STATUS")).thenReturn(0);

        OrderBusinessException closed = assertThrows(OrderBusinessException.class,
                () -> orderService.payment(request("order-1")));
        assertEquals(MessageConstant.SHOP_CLOSED_FOR_IMMEDIATE_PAYMENT, closed.getMessage());
        verify(weChatPayUtil, never()).pay(any(), any(), any(), any());

        when(valueOperations.get("SHOP STATUS")).thenReturn(null);
        assertThrows(OrderBusinessException.class, () -> orderService.payment(request("order-1")));
        doThrow(new IllegalStateException("redis unavailable")).when(valueOperations).get("SHOP STATUS");
        assertThrows(OrderBusinessException.class, () -> orderService.payment(request("order-1")));
        verify(weChatPayUtil, never()).pay(any(), any(), any(), any());
    }

    @Test
    void immediatePaymentShouldAllowTheClosingTimeBoundary() throws Exception {
        BaseContext.setCurrentId(1L);
        Clock closingClock = Clock.fixed(Instant.parse("2026-10-11T14:00:00Z"), ZoneId.of("UTC"));
        ReflectionTestUtils.setField(orderService, "clock", closingClock);
        ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService",
                new DeliveryTimeValidationService(properties, closingClock));
        when(orderMapper.getByNumberAndUserId("order-1", 1L)).thenReturn(order(Orders.PENDING_PAYMENT,
                Orders.UN_PAID, 1, LocalDateTime.of(2026, 10, 11, 21, 55), null));
        when(valueOperations.get("SHOP STATUS")).thenReturn(1);
        when(userMapper.getById(1L)).thenReturn(new User());
        when(weChatPayUtil.pay(eq("order-1"), any(BigDecimal.class), any(), any())).thenReturn(new com.alibaba.fastjson.JSONObject());

        orderService.payment(request("order-1"));

        verify(weChatPayUtil).pay(eq("order-1"), any(BigDecimal.class), any(), any());
    }

    @Test
    void scheduledPaymentCanProceedWhileShopIsClosedWhenAppointmentRemainsValid() throws Exception {
        BaseContext.setCurrentId(1L);
        Clock closedClock = Clock.fixed(Instant.parse("2026-10-11T15:00:00Z"), ZoneId.of("UTC"));
        ReflectionTestUtils.setField(orderService, "clock", closedClock);
        ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService",
                new DeliveryTimeValidationService(properties, closedClock));
        when(orderMapper.getByNumberAndUserId("order-1", 1L)).thenReturn(order(Orders.PENDING_PAYMENT,
                Orders.UN_PAID, 0, LocalDateTime.ofInstant(closedClock.instant(), ZoneId.systemDefault()).minusMinutes(5),
                LocalDateTime.of(2026, 10, 12, 12, 0)));
        when(userMapper.getById(1L)).thenReturn(new User());
        when(weChatPayUtil.pay(eq("order-1"), any(BigDecimal.class), any(), any())).thenReturn(new com.alibaba.fastjson.JSONObject());

        orderService.payment(request("order-1"));

        verify(valueOperations, never()).get("SHOP STATUS");
        verify(weChatPayUtil).pay(eq("order-1"), any(BigDecimal.class), any(), any());
    }

    @Test
    void scheduledPaymentShouldRejectWhenAppointmentIsTooSoon() throws Exception {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByNumberAndUserId("order-1", 1L)).thenReturn(order(Orders.PENDING_PAYMENT,
                Orders.UN_PAID, 0, LocalDateTime.of(2026, 10, 11, 17, 55),
                LocalDateTime.of(2026, 10, 11, 18, 10)));

        assertThrows(OrderBusinessException.class, () -> orderService.payment(request("order-1")));
        verify(weChatPayUtil, never()).pay(any(), any(), any(), any());
    }

    @Test
    void paymentDeadlineAllowsExactlyFifteenMinutesButRejectsAfterward() throws Exception {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByNumberAndUserId("order-1", 1L)).thenReturn(order(Orders.PENDING_PAYMENT,
                Orders.UN_PAID, 0, storedNow().minusMinutes(15),
                LocalDateTime.of(2026, 10, 12, 12, 0)));
        when(userMapper.getById(1L)).thenReturn(new User());
        when(weChatPayUtil.pay(eq("order-1"), any(BigDecimal.class), any(), any())).thenReturn(new com.alibaba.fastjson.JSONObject());

        orderService.payment(request("order-1"));

        verify(weChatPayUtil).pay(eq("order-1"), any(BigDecimal.class), any(), any());
        when(orderMapper.getByNumberAndUserId("order-2", 1L)).thenReturn(order(Orders.PENDING_PAYMENT,
                Orders.UN_PAID, 0, storedNow().minusMinutes(15).minusSeconds(1),
                LocalDateTime.of(2026, 10, 12, 12, 0)));
        assertThrows(OrderBusinessException.class, () -> orderService.payment(request("order-2")));
        verify(weChatPayUtil, never()).pay(eq("order-2"), any(), any(), any());
    }

    @Test
    void paymentDeadlineMatchesOrderLocalTimeWhenJvmZoneIsNotBeijing() throws Exception {
        TimeZone originalTimeZone = TimeZone.getDefault();
        ZoneId jvmZone = ZoneId.of("America/Los_Angeles");
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(jvmZone));
            BaseContext.setCurrentId(1L);
            LocalDateTime createdAt = LocalDateTime.ofInstant(NOW.minusSeconds(15 * 60L), jvmZone);
            when(orderMapper.getByNumberAndUserId("order-1", 1L)).thenReturn(order(Orders.PENDING_PAYMENT,
                    Orders.UN_PAID, 0, createdAt, LocalDateTime.of(2026, 10, 12, 12, 0)));
            when(userMapper.getById(1L)).thenReturn(new User());
            when(weChatPayUtil.pay(eq("order-1"), any(BigDecimal.class), any(), any()))
                    .thenReturn(new com.alibaba.fastjson.JSONObject());

            orderService.payment(request("order-1"));

            verify(weChatPayUtil).pay(eq("order-1"), any(BigDecimal.class), any(), any());
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    void cancelledPaidAndExpiredOrdersCannotStartPayment() throws Exception {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByNumberAndUserId("cancelled", 1L)).thenReturn(order(Orders.CANCELLED,
                Orders.UN_PAID, 0, LocalDateTime.of(2026, 10, 11, 17, 55), LocalDateTime.of(2026, 10, 12, 12, 0)));
        when(orderMapper.getByNumberAndUserId("paid", 1L)).thenReturn(order(Orders.TO_BE_CONFIRMED,
                Orders.PAID, 0, LocalDateTime.of(2026, 10, 11, 17, 55), LocalDateTime.of(2026, 10, 12, 12, 0)));
        assertThrows(OrderBusinessException.class, () -> orderService.payment(request("cancelled")));
        assertThrows(OrderBusinessException.class, () -> orderService.payment(request("paid")));
        verify(weChatPayUtil, never()).pay(any(), any(), any(), any());
    }

    @Test
    void successfulImmediatePaymentUpdatesEstimateUsingTrustedWechatSuccessTime() {
        Orders pending = order(Orders.PENDING_PAYMENT, Orders.UN_PAID, 1,
                LocalDateTime.of(2026, 10, 11, 17, 0), null);
        when(orderMapper.getByNumber("order-1")).thenReturn(pending);
        when(orderMapper.updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class))).thenReturn(1);

        orderService.paySuccess("order-1", OffsetDateTime.parse("2026-10-11T17:18:00+08:00"));

        ArgumentCaptor<Orders> target = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), target.capture());
        assertEquals(LocalDateTime.of(2026, 10, 11, 17, 18), target.getValue().getCheckoutTime());
        assertEquals(LocalDateTime.of(2026, 10, 11, 17, 48), target.getValue().getEstimatedDeliveryTime());
        verify(webSocketServer).sendToAllClient(any());
    }

    @Test
    void simulatedImmediatePaymentUsesSimulationSuccessTimeForEstimate() {
        BaseContext.setCurrentId(1L);
        Orders pending = order(Orders.PENDING_PAYMENT, Orders.UN_PAID, 1,
                LocalDateTime.of(2026, 10, 11, 17, 55), null);
        when(orderMapper.getByIdAndUserId(1L, 1L)).thenReturn(pending);
        when(valueOperations.get("SHOP STATUS")).thenReturn(1);
        when(orderMapper.updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class))).thenReturn(1);

        orderService.mockPaySuccessForUser("order-1", 1L);

        ArgumentCaptor<Orders> target = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), target.capture());
        assertEquals(LocalDateTime.of(2026, 10, 11, 18, 0), target.getValue().getCheckoutTime());
        assertEquals(LocalDateTime.of(2026, 10, 11, 18, 30), target.getValue().getEstimatedDeliveryTime());
    }

    @Test
    void repeatedSimulationCannotUpdateOrExtendImmediateEstimate() {
        BaseContext.setCurrentId(1L);
        Orders pending = order(Orders.PENDING_PAYMENT, Orders.UN_PAID, 1,
                LocalDateTime.of(2026, 10, 11, 17, 55), null);
        Orders paid = order(Orders.TO_BE_CONFIRMED, Orders.PAID, 1,
                LocalDateTime.of(2026, 10, 11, 17, 55), LocalDateTime.of(2026, 10, 11, 18, 30));
        when(orderMapper.getByIdAndUserId(1L, 1L)).thenReturn(pending, paid);
        when(valueOperations.get("SHOP STATUS")).thenReturn(1);
        when(orderMapper.updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class))).thenReturn(1);

        orderService.mockPaySuccessForUser("order-1", 1L);
        assertThrows(OrderBusinessException.class, () -> orderService.mockPaySuccessForUser("order-1", 1L));

        ArgumentCaptor<Orders> target = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), target.capture());
        assertEquals(LocalDateTime.of(2026, 10, 11, 18, 30), target.getValue().getEstimatedDeliveryTime());
        verify(webSocketServer).sendToAllClient(any());
    }

    @Test
    void repeatedImmediatePaymentCallbackCannotMoveEstimateAgain() {
        Orders pending = order(Orders.PENDING_PAYMENT, Orders.UN_PAID, 1,
                LocalDateTime.of(2026, 10, 11, 17, 0), null);
        when(orderMapper.getByNumber("order-1")).thenReturn(pending, pending);
        when(orderMapper.updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class))).thenReturn(1, 0);
        when(orderMapper.getById(1L)).thenReturn(order(Orders.TO_BE_CONFIRMED, Orders.PAID, 1,
                LocalDateTime.of(2026, 10, 11, 17, 0), LocalDateTime.of(2026, 10, 11, 17, 48)));

        orderService.paySuccess("order-1", OffsetDateTime.parse("2026-10-11T17:18:00+08:00"));
        orderService.paySuccess("order-1", OffsetDateTime.parse("2026-10-11T17:25:00+08:00"));

        ArgumentCaptor<Orders> targets = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper, org.mockito.Mockito.times(2)).updateStatusIfCurrent(eq(1L),
                eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID), isNull(), targets.capture());
        assertEquals(LocalDateTime.of(2026, 10, 11, 17, 48), targets.getAllValues().get(0).getEstimatedDeliveryTime());
        assertEquals(LocalDateTime.of(2026, 10, 11, 17, 55), targets.getAllValues().get(1).getEstimatedDeliveryTime());
        verify(webSocketServer).sendToAllClient(any());
    }

    @Test
    void successfulScheduledPaymentDoesNotChangeAppointment() {
        LocalDateTime appointment = LocalDateTime.of(2026, 10, 12, 12, 0);
        Orders pending = order(Orders.PENDING_PAYMENT, Orders.UN_PAID, 0,
                LocalDateTime.of(2026, 10, 11, 17, 0), appointment);
        when(orderMapper.getByNumber("order-1")).thenReturn(pending);
        when(orderMapper.updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class))).thenReturn(1);

        orderService.paySuccess("order-1", OffsetDateTime.parse("2026-10-11T17:18:00+08:00"));

        ArgumentCaptor<Orders> target = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), target.capture());
        assertNull(target.getValue().getEstimatedDeliveryTime());
        verify(webSocketServer).sendToAllClient(any());
    }

    @Test
    void successTimeWithUtcOffsetIsConvertedToBeijingBeforeSaving() {
        Orders pending = order(Orders.PENDING_PAYMENT, Orders.UN_PAID, 1,
                LocalDateTime.of(2026, 10, 11, 17, 0), null);
        when(orderMapper.getByNumber("order-1")).thenReturn(pending);
        when(orderMapper.updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class))).thenReturn(1);

        orderService.paySuccess("order-1", OffsetDateTime.parse("2026-10-11T09:18:00Z"));

        ArgumentCaptor<Orders> target = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateStatusIfCurrent(eq(1L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), target.capture());
        assertEquals(LocalDateTime.of(2026, 10, 11, 17, 18), target.getValue().getCheckoutTime());
        assertEquals(LocalDateTime.of(2026, 10, 11, 17, 48), target.getValue().getEstimatedDeliveryTime());
    }

    private OrdersPaymentDTO request(String orderNumber) {
        OrdersPaymentDTO request = new OrdersPaymentDTO();
        request.setOrderNumber(orderNumber);
        return request;
    }

    private LocalDateTime storedNow() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }

    private Orders order(Integer status, Integer payStatus, Integer deliveryStatus,
                         LocalDateTime orderTime, LocalDateTime estimatedDeliveryTime) {
        return Orders.builder().id(1L).number("order-1").userId(1L).status(status).payStatus(payStatus)
                .deliveryStatus(deliveryStatus).orderTime(orderTime).estimatedDeliveryTime(estimatedDeliveryTime)
                .amount(BigDecimal.TEN).build();
    }
}
