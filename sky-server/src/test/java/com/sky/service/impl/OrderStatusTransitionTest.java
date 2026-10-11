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
 * 验证订单状态变更必须匹配数据库中的预期状态
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
     * 清理测试线程中的当前用户身份
     */
    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    /**
     * 验证待付款订单支付成功后原子变为待接单并发送一次通知
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
     * 验证迟到的支付通知不能恢复已经取消的订单
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
     * 验证支付成功与超时取消竞争时超时任务会再次检查未支付状态
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
     * 验证没有超时待付款订单时不会执行条件取消更新
     */
    @Test
    void timeoutTaskMustNotUpdateWhenNoEligibleOrdersAreFound() {
        when(orderMapper.getByStatusAndOrderTimeLT(eq(Orders.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(java.util.Collections.emptyList());

        orderTask.processTimeoutOrder();

        verify(orderMapper, never()).updateStatusIfCurrent(anyLong(), anyInt(), anyInt(), any(), any(Orders.class));
    }

    /**
     * 验证重复支付回调在条件更新失败后不重复发送来单通知
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
     * 验证管理端重复接单或接收已取消订单时影响行数为零并拒绝操作
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
     * 验证不合法的派送状态转换在数据库条件更新失败时被拒绝
     */
    @Test
    void invalidDeliveryTransitionMustBeRejectedWhenUpdateAffectsNoRows() {
        when(orderMapper.updateStatusIfCurrent(eq(10L), eq(Orders.CONFIRMED), eq(Orders.PAID),
                isNull(), any(Orders.class))).thenReturn(0);

        assertThrows(OrderBusinessException.class, () -> orderService.delivery(10L));

        verify(orderMapper).updateStatusIfCurrent(eq(10L), eq(Orders.CONFIRMED), eq(Orders.PAID),
                isNull(), any(Orders.class));
    }

    /**
     * 验证支付回调先赢得竞争时用户取消不会退款或覆盖已支付状态
     *
     * @throws Exception 取消操作异常
     */
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

    /**
     * 验证支付竞争中条件更新失败时服务层不执行退款等后续操作
     *
     * @throws Exception 取消操作异常
     */
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

    /**
     * 验证订单条件更新 XML 可以解析参数并包含订单状态、支付状态和超时时间条件
     *
     * @throws Exception Mapper XML 解析异常
     */
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
     * 构造订单状态测试所需的订单对象
     *
     * @param id 订单ID
     * @param number 订单编号
     * @param status 订单业务状态
     * @param payStatus 订单支付状态
     * @return 构造完成的订单
     */
    private Orders order(Long id, String number, Integer status, Integer payStatus) {
        return Orders.builder().id(id).number(number).status(status).payStatus(payStatus).build();
    }
}
