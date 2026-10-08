package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.utils.WeChatPayUtil;
import com.sky.websocket.WebSocketServer;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceOwnershipTest {

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

    @Test
    void submitOrderShouldRejectAnotherUsersAddressBeforeAnyOrderSideEffect() {
        BaseContext.setCurrentId(1L);
        when(addressBookMapper.getByIdAndUserId(20L, 1L)).thenReturn(null);
        OrdersSubmitDTO request = new OrdersSubmitDTO();
        request.setAddressBookId(20L);

        assertThrows(AddressBookBusinessException.class, () -> orderService.submitOrder(request));

        verify(orderMapper, never()).insert(any(Orders.class));
        verify(orderDetailMapper, never()).insertBatch(any());
        verify(shoppingCartMapper, never()).deleteByUserId(any());
        verify(addressBookMapper, never()).getById(20L);
    }

    @Test
    void detailsShouldRejectAnotherUsersOrderBeforeLoadingItsDetails() {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByIdAndUserId(20L, 1L)).thenReturn(null);

        assertThrows(OrderBusinessException.class, () -> orderService.detailsForUser(20L));

        verify(orderDetailMapper, never()).getByOrderId(20L);
        verify(orderMapper, never()).getById(20L);
    }

    @Test
    void detailsShouldReturnTheCurrentUsersOrder() {
        BaseContext.setCurrentId(1L);
        Orders ownOrder = Orders.builder().id(10L).userId(1L).address("saved address").build();
        when(orderMapper.getByIdAndUserId(10L, 1L)).thenReturn(ownOrder);
        when(orderDetailMapper.getByOrderId(10L)).thenReturn(Collections.emptyList());

        assertEquals(10L, orderService.detailsForUser(10L).getId());

        verify(orderDetailMapper).getByOrderId(10L);
    }

    @Test
    void cancellationShouldRejectAnotherUsersOrderWithoutUpdatingIt() {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByIdAndUserId(20L, 1L)).thenReturn(null);

        assertThrows(OrderBusinessException.class, () -> orderService.userCancelById(20L));

        verify(orderMapper, never()).update(any(Orders.class));
    }

    @Test
    void cancellationShouldAllowCurrentUsersUnpaidOrder() throws Exception {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByIdAndUserId(10L, 1L)).thenReturn(Orders.builder()
                .id(10L).userId(1L).status(Orders.PENDING_PAYMENT).payStatus(Orders.UN_PAID).build());

        orderService.userCancelById(10L);

        ArgumentCaptor<Orders> update = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).update(update.capture());
        assertEquals(Orders.CANCELLED, update.getValue().getStatus());
    }

    @Test
    void repetitionShouldRejectAnotherUsersOrderWithoutReadingOrInsertingDetails() {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByIdAndUserId(20L, 1L)).thenReturn(null);

        assertThrows(OrderBusinessException.class, () -> orderService.repetition(20L));

        verify(orderDetailMapper, never()).getByOrderId(20L);
        verify(shoppingCartMapper, never()).insertBatch(any());
    }

    @Test
    void repetitionShouldCopyDetailsForCurrentUsersOrder() {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByIdAndUserId(10L, 1L))
                .thenReturn(Orders.builder().id(10L).userId(1L).build());
        when(orderDetailMapper.getByOrderId(10L)).thenReturn(Collections.singletonList(
                OrderDetail.builder().name("dish").number(2).build()));

        orderService.repetition(10L);

        verify(shoppingCartMapper).insertBatch(any());
    }

    @Test
    void paymentShouldRejectAnotherUsersOrderBeforeCallingPaymentProvider() throws Exception {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByNumberAndUserId("order-20", 1L)).thenReturn(null);
        OrdersPaymentDTO request = new OrdersPaymentDTO();
        request.setOrderNumber("order-20");

        assertThrows(OrderBusinessException.class, () -> orderService.payment(request));

        verify(userMapper, never()).getById(1L);
        verify(weChatPayUtil, never()).pay(any(), any(), any(), any());
    }

    @Test
    void mockPaymentShouldRejectAnotherUsersOrderWithoutChangingIt() {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByIdAndUserId(20L, 1L)).thenReturn(null);

        assertThrows(OrderBusinessException.class,
                () -> orderService.mockPaySuccessForUser("order-20", 20L));

        verify(orderMapper, never()).update(any(Orders.class));
        verify(webSocketServer, never()).sendToAllClient(any());
    }

    @Test
    void mockPaymentShouldProcessCurrentUsersMatchingOrder() {
        BaseContext.setCurrentId(1L);
        Orders ownOrder = Orders.builder().id(10L).userId(1L).number("order-10").build();
        when(orderMapper.getByIdAndUserId(10L, 1L)).thenReturn(ownOrder);

        orderService.mockPaySuccessForUser("order-10", 10L);

        verify(orderMapper).update(any(Orders.class));
        verify(webSocketServer).sendToAllClient(any());
    }

    @Test
    void reminderShouldRejectAnotherUsersOrderWithoutSendingNotification() {
        BaseContext.setCurrentId(1L);
        when(orderMapper.getByIdAndUserId(20L, 1L)).thenReturn(null);

        assertThrows(OrderBusinessException.class, () -> orderService.reminder(20L));

        verify(webSocketServer, never()).sendToAllClient(any());
    }
}
