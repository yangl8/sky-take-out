package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.result.PageResult;
import com.sky.vo.OrderVO;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaginationBatchTest {

    private static final Long USER_ID = 9001L;

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;
    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(USER_ID);
        ReflectionTestUtils.setField(orderService, "deliveryFee", new BigDecimal("2.50"));
    }

    @AfterEach
    void tearDown() {
        PageHelper.clearPage();
        BaseContext.removeCurrentId();
    }

    @Test
    void shouldGroupDetailsByOrderAndPreservePageAndDetailOrder() {
        Page<Orders> page = page(1, 4, 28, order(2L), order(1L), order(3L), order(4L));
        when(orderMapper.pageQuery(any(OrdersPageQueryDTO.class))).thenReturn(page);
        when(orderDetailMapper.getByOrderIds(Arrays.asList(2L, 1L, 3L, 4L)))
                .thenReturn(Arrays.asList(detail(22L, 2L), detail(11L, 1L), detail(21L, 2L)));

        PageResult result = orderService.pageQuery4User(1, 4, 3, 1);

        assertEquals(28L, result.getTotal());
        List<OrderVO> records = result.getRecords();
        assertEquals(Arrays.asList(2L, 1L, 3L, 4L), Arrays.asList(
                records.get(0).getId(), records.get(1).getId(), records.get(2).getId(), records.get(3).getId()));
        assertEquals(Arrays.asList(22L, 21L), Arrays.asList(
                records.get(0).getOrderDetailList().get(0).getId(),
                records.get(0).getOrderDetailList().get(1).getId()));
        assertEquals(11L, records.get(1).getOrderDetailList().get(0).getId());
        assertTrue(records.get(2).getOrderDetailList().isEmpty());
        assertTrue(records.get(3).getOrderDetailList().isEmpty());
        assertNotSame(records.get(2).getOrderDetailList(), records.get(3).getOrderDetailList());
        assertEquals(new BigDecimal("2.50"), records.get(0).getDeliveryFee());

        ArgumentCaptor<OrdersPageQueryDTO> queryCaptor = ArgumentCaptor.forClass(OrdersPageQueryDTO.class);
        verify(orderMapper).pageQuery(queryCaptor.capture());
        assertEquals(USER_ID, queryCaptor.getValue().getUserId());
        assertEquals(3, queryCaptor.getValue().getStatus());
        assertEquals(1, queryCaptor.getValue().getPayStatus());
        verify(orderDetailMapper).getByOrderIds(Arrays.asList(2L, 1L, 3L, 4L));
        verify(orderDetailMapper, never()).getByOrderId(any());
    }

    @Test
    void shouldBatchOnlySecondPageOrdersAfterPageHelperClearsPagination() {
        Page<Orders> secondPage = page(2, 2, 5, order(103L), order(104L));
        when(orderMapper.pageQuery(any(OrdersPageQueryDTO.class))).thenAnswer(invocation -> {
            Page<?> pageRequest = PageHelper.getLocalPage();
            assertNotNull(pageRequest);
            assertEquals(2, pageRequest.getPageNum());
            assertEquals(2, pageRequest.getPageSize());
            PageHelper.clearPage();
            return secondPage;
        });
        when(orderDetailMapper.getByOrderIds(Arrays.asList(103L, 104L))).thenAnswer(invocation -> {
            assertNull(PageHelper.getLocalPage());
            assertEquals(Arrays.asList(103L, 104L), invocation.getArgument(0));
            return Arrays.asList(detail(301L, 103L), detail(302L, 103L), detail(401L, 104L));
        });

        PageResult result = orderService.pageQuery4User(2, 2, null, null);

        assertEquals(5L, result.getTotal());
        List<OrderVO> records = result.getRecords();
        assertEquals(2, records.size());
        assertEquals(Arrays.asList(103L, 104L), Arrays.asList(records.get(0).getId(), records.get(1).getId()));
        assertEquals(Arrays.asList(301L, 302L), Arrays.asList(
                records.get(0).getOrderDetailList().get(0).getId(),
                records.get(0).getOrderDetailList().get(1).getId()));
        assertEquals(Collections.singletonList(401L), Collections.singletonList(
                records.get(1).getOrderDetailList().get(0).getId()));
        assertEquals(3, records.get(0).getOrderDetailList().size() + records.get(1).getOrderDetailList().size());
        verify(orderDetailMapper).getByOrderIds(Arrays.asList(103L, 104L));
        verify(orderDetailMapper, never()).getByOrderId(any());
    }

    @Test
    void shouldReturnSingleOrderWithEmptyDetails() {
        when(orderMapper.pageQuery(any(OrdersPageQueryDTO.class))).thenReturn(page(1, 10, 1, order(8L)));
        when(orderDetailMapper.getByOrderIds(Collections.singletonList(8L))).thenReturn(Collections.emptyList());

        PageResult result = orderService.pageQuery4User(1, 10, null, null);

        assertEquals(1L, result.getTotal());
        List<OrderVO> records = result.getRecords();
        assertEquals(1, records.size());
        assertTrue(records.get(0).getOrderDetailList().isEmpty());
        verify(orderDetailMapper).getByOrderIds(Collections.singletonList(8L));
        verify(orderDetailMapper, never()).getByOrderId(any());
    }

    @Test
    void shouldPreserveTotalAndSkipDetailQueryForEmptyPage() {
        when(orderMapper.pageQuery(any(OrdersPageQueryDTO.class))).thenReturn(page(4, 10, 12));

        PageResult result = orderService.pageQuery4User(4, 10, null, null);

        assertEquals(12L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(orderDetailMapper, never()).getByOrderIds(any());
        verify(orderDetailMapper, never()).getByOrderId(any());
    }

    @Test
    void adminDeliverySearchShouldApplyEtaQueryBeforeSecondPageFetch() {
        OrdersPageQueryDTO query = new OrdersPageQueryDTO();
        query.setPage(2);
        query.setPageSize(2);
        query.setStatus(3);
        query.setNumber("ETA-");
        query.setPhone("123");
        when(orderMapper.pageQuery(any(OrdersPageQueryDTO.class))).thenAnswer(invocation -> {
            Page<?> pageRequest = PageHelper.getLocalPage();
            assertNotNull(pageRequest);
            assertEquals(2, pageRequest.getPageNum());
            assertEquals(2, pageRequest.getPageSize());
            OrdersPageQueryDTO mapperQuery = invocation.getArgument(0);
            assertEquals(3, mapperQuery.getStatus());
            assertEquals("ETA-", mapperQuery.getNumber());
            assertEquals("123", mapperQuery.getPhone());
            PageHelper.clearPage();
            return page(2, 2, 5);
        });

        PageResult result = orderService.conditionSearch(query);

        assertEquals(5L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(orderMapper).pageQuery(query);
    }

    private Page<Orders> page(int pageNum, int pageSize, long total, Orders... orders) {
        Page<Orders> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.addAll(Arrays.asList(orders));
        return page;
    }

    private Orders order(Long id) {
        return Orders.builder().id(id).number("ORDER-" + id).build();
    }

    private OrderDetail detail(Long id, Long orderId) {
        return OrderDetail.builder().id(id).orderId(orderId).name("Dish " + id).build();
    }
}
