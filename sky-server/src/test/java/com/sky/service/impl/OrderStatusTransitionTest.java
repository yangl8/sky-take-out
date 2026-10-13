package com.sky.service.impl;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import com.sky.context.BaseContext;
import com.sky.config.DeliveryTimeProperties;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.task.OrderTask;
import com.sky.utils.WeChatPayUtil;
import com.sky.websocket.WebSocketServer;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies order transitions require the expected persisted state
 */
@ExtendWith(MockitoExtension.class)
class OrderStatusTransitionTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderDetailMapper orderDetailMapper;
    @Mock private AddressBookMapper addressBookMapper;
    @Mock private ShoppingCartMapper shoppingCartMapper;
    @Mock private UserMapper userMapper;
    @Mock private WeChatPayUtil weChatPayUtil;
    @Mock private WebSocketServer webSocketServer;
    @Mock private Environment environment;
    @Mock private DeliveryTimeProperties deliveryTimeProperties;

    @InjectMocks
    private OrderServiceImpl orderService;

    @InjectMocks
    private OrderTask orderTask;

    @BeforeEach
    void setUpTimeZone() {
        lenient().when(deliveryTimeProperties.getZoneId()).thenReturn(ZoneId.of("Asia/Shanghai"));
        lenient().when(deliveryTimeProperties.getMinimumPreparationMinutes()).thenReturn(30);
    }

    /**
     * Clears the current user identity from the test thread
     */
    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    /**
     * Verifies successful payment atomically advances the order and sends one notification
     */
    @Test
    void paymentSuccessShouldMovePendingUnpaidOrderAndNotifyOnce() {
        Orders pendingOrder = order(10L, "order-10", Orders.PENDING_PAYMENT, Orders.UN_PAID);
        when(orderMapper.getByNumber("order-10")).thenReturn(pendingOrder);
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class))).thenReturn(1);

        orderService.paySuccess("order-10", OffsetDateTime.parse("2026-10-11T17:18:00+08:00"));

        ArgumentCaptor<Orders> targetOrder = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateStatusIfCurrent(eq(10L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), targetOrder.capture());
        assertEquals(Orders.TO_BE_CONFIRMED, targetOrder.getValue().getStatus());
        assertEquals(Orders.PAID, targetOrder.getValue().getPayStatus());
        assertNotNull(targetOrder.getValue().getCheckoutTime());
        verify(webSocketServer).sendToAllClient(any());
    }

    /**
     * Verifies a late payment notification cannot restore a cancelled order
     */
    @Test
    void latePaymentNotificationMustNotRestoreCancelledOrder() {
        when(orderMapper.getByNumber("order-10"))
                .thenReturn(order(10L, "order-10", Orders.CANCELLED, Orders.UN_PAID));

        orderService.paySuccess("order-10", OffsetDateTime.parse("2026-10-11T17:18:00+08:00"));

        verify(orderMapper, never()).updateStatusIfCurrent(anyLong(), anyInt(), anyInt(), any(), any(Orders.class));
        verify(webSocketServer, never()).sendToAllClient(any());
    }

    /**
     * Verifies timeout cancellation rechecks the unpaid state when racing with payment
     */
    @Test
    void timeoutTaskMustNotCancelOrderAfterPaymentWinsTheRace() {
        Orders stalePendingOrder = order(10L, "order-10", Orders.PENDING_PAYMENT, Orders.UN_PAID);
        when(orderMapper.getByStatusAndOrderTimeLT(eq(Orders.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.singletonList(stalePendingOrder));
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                any(LocalDateTime.class), any(Orders.class))).thenReturn(0);

        orderTask.processTimeoutOrder();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderMapper).getByStatusAndOrderTimeLT(eq(Orders.PENDING_PAYMENT), cutoffCaptor.capture());
        LocalDateTime timeoutCutoff = cutoffCaptor.getValue();
        assertTrue(!timeoutCutoff.isAfter(LocalDateTime.now().minusMinutes(15)));

        ArgumentCaptor<Orders> targetOrder = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateStatusIfCurrent(eq(10L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                eq(timeoutCutoff), targetOrder.capture());
        assertEquals(Orders.CANCELLED, targetOrder.getValue().getStatus());
        assertEquals(Orders.UN_PAID, targetOrder.getValue().getPayStatus());
    }

    /**
     * Verifies no conditional cancellation update runs when no unpaid order has timed out
     */
    @Test
    void timeoutTaskMustNotUpdateWhenNoEligibleOrdersAreFound() {
        when(orderMapper.getByStatusAndOrderTimeLT(eq(Orders.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.emptyList());

        orderTask.processTimeoutOrder();

        verify(orderMapper, never()).updateStatusIfCurrent(anyLong(), anyInt(), anyInt(), any(), any(Orders.class));
    }

    /**
     * Verifies a duplicate payment callback does not send another notification after its conditional update fails
     */
    @Test
    void repeatedPaymentNotificationMustNotSendDuplicateNoticeWhenUpdateAffectsNoRows() {
        Orders pendingOrder = order(10L, "order-10", Orders.PENDING_PAYMENT, Orders.UN_PAID);
        Orders alreadyPaidOrder = order(10L, "order-10", Orders.TO_BE_CONFIRMED, Orders.PAID);
        when(orderMapper.getByNumber("order-10")).thenReturn(pendingOrder);
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class))).thenReturn(0);
        when(orderMapper.getById(10L)).thenReturn(alreadyPaidOrder);

        orderService.paySuccess("order-10", OffsetDateTime.parse("2026-10-11T17:18:00+08:00"));

        verify(orderMapper).updateStatusIfCurrent(eq(10L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class));
        verify(webSocketServer, never()).sendToAllClient(any());
    }

    /**
     * Verifies duplicate acceptance and acceptance of cancelled orders are rejected when no row is updated
     */
    @Test
    void adminMustNotAcceptCancelledOrderWhenConditionalUpdateAffectsNoRows() {
        OrdersConfirmDTO request = new OrdersConfirmDTO();
        request.setId(10L);
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID),
                isNull(), any(Orders.class))).thenReturn(0);

        assertThrows(OrderBusinessException.class, () -> orderService.confirm(request));

        ArgumentCaptor<Orders> targetOrder = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateStatusIfCurrent(eq(10L), eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID),
                isNull(), targetOrder.capture());
        assertEquals(Orders.CONFIRMED, targetOrder.getValue().getStatus());
        verify(webSocketServer, never()).sendToAllClient(any());
    }

    /**
     * Verifies invalid delivery transitions are rejected when the conditional update fails
     */
    @Test
    void invalidDeliveryTransitionMustBeRejectedWhenUpdateAffectsNoRows() {
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.CONFIRMED), eq(Orders.PAID),
                isNull(), any(Orders.class))).thenReturn(0);

        assertThrows(OrderBusinessException.class, () -> orderService.delivery(10L));

        verify(orderMapper).updateStatusIfCurrent(eq(10L), eq(Orders.CONFIRMED), eq(Orders.PAID),
                isNull(), any(Orders.class));
    }

    @Test
    void userCancellationMustNotOverrideConcurrentPaymentSuccess() throws Exception {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByIdAndUserId(10L, 1L))
                .thenReturn(order(10L, "order-10", Orders.PENDING_PAYMENT, Orders.UN_PAID));
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.PENDING_PAYMENT), eq(Orders.UN_PAID),
                isNull(), any(Orders.class))).thenReturn(0);

        assertThrows(OrderBusinessException.class, () -> orderService.userCancelById(10L));

        verify(weChatPayUtil, never()).refund(any(), any(), any(), any());
        verify(webSocketServer, never()).sendToAllClient(any());
    }

    @Test
    void failedPaidCancellationUpdateMustNotCallRefundProvider() throws Exception {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByIdAndUserId(10L, 1L))
                .thenReturn(order(10L, "order-10", Orders.TO_BE_CONFIRMED, Orders.PAID));
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.TO_BE_CONFIRMED), eq(Orders.PAID),
                isNull(), any(Orders.class))).thenReturn(0);

        assertThrows(OrderBusinessException.class, () -> orderService.userCancelById(10L));

        verify(weChatPayUtil, never()).refund(any(), any(), any(), any());
    }

    @Test
    void mapperSqlMustBindExpectedStatesAndOptionalTimeout() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("Orders", Orders.class);
        configuration.addMapper(OrderMapper.class);
        try (InputStream mapperXml = getClass().getClassLoader().getResourceAsStream("mapper/OrderMapper.xml")) {
            new XMLMapperBuilder(mapperXml, configuration, "mapper/OrderMapper.xml",
                    configuration.getSqlFragments()).parse();
        }

        Orders targetOrder = Orders.builder()
                .status(Orders.CANCELLED)
                .payStatus(Orders.UN_PAID)
                .checkoutTime(LocalDateTime.of(2026, 10, 11, 18, 0))
                .cancelTime(LocalDateTime.now())
                .estimatedDeliveryTime(LocalDateTime.of(2026, 10, 12, 12, 0))
                .build();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", 10L);
        parameters.put("expectedStatus", Orders.PENDING_PAYMENT);
        parameters.put("expectedPayStatus", Orders.UN_PAID);
        parameters.put("orderTimeBefore", LocalDateTime.now());
        parameters.put("targetOrder", targetOrder);

        MappedStatement statement = configuration.getMappedStatement(
                "com.sky.mapper.OrderMapper.updateStatusIfCurrent");
        BoundSql boundSql = statement.getBoundSql(parameters);
        String sql = boundSql.getSql().replaceAll("\\s+", " ").toLowerCase();

        assertTrue(sql.contains("where id = ? and status = ? and pay_status = ? and order_time < ?"));
        assertTrue(sql.contains("checkout_time=?"));
        assertTrue(sql.contains("estimated_delivery_time = ?"));
        assertTrue(boundSql.getParameterMappings().stream()
                .map(parameterMapping -> parameterMapping.getProperty())
                .anyMatch("expectedStatus"::equals));
        assertTrue(boundSql.getParameterMappings().stream()
                .map(parameterMapping -> parameterMapping.getProperty())
                .anyMatch("expectedPayStatus"::equals));
        assertTrue(boundSql.getParameterMappings().stream()
                .map(parameterMapping -> parameterMapping.getProperty())
                .anyMatch("orderTimeBefore"::equals));
    }

    /**
     * Creates an order fixture for transition tests
     *
     * @param id Order ID
     * @param number Order number
     * @param status Business status
     * @param payStatus Payment status
     * @return Constructed order
     */
    private Orders order(Long id, String number, Integer status, Integer payStatus) {
        return Orders.builder().id(id).number(number).status(status).payStatus(payStatus).build();
    }
}
