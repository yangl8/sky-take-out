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

/**
 * 用户端订单分页批量查询明细回归测试
 */
@ExtendWith(MockitoExtension.class)
class OrderPaginationBatchTest {

    private static final Long USER_ID = 9001L;

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;
    @InjectMocks
    private OrderServiceImpl orderService;

    /**
     * 设置用户身份和配送费
     */
    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(USER_ID);
        ReflectionTestUtils.setField(orderService, "deliveryFee", new BigDecimal("2.50"));
    }

    /**
     * 清理分页状态和当前用户身份
     */
    @AfterEach
    void tearDown() {
        PageHelper.clearPage();
        BaseContext.removeCurrentId();
    }

    /**
     * 按订单关联批量明细并保持订单及明细顺序和分页总数
     */
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

    /**
     * 第2页只批量查询当前页订单并在分页上下文清理后读取明细
     */
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

    /**
     * 单条订单没有明细时返回独立的空集合
     */
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

    /**
     * 空页保留 COUNT 总数且不发起明细查询
     */
    @Test
    void shouldPreserveTotalAndSkipDetailQueryForEmptyPage() {
        when(orderMapper.pageQuery(any(OrdersPageQueryDTO.class))).thenReturn(page(4, 10, 12));

        PageResult result = orderService.pageQuery4User(4, 10, null, null);

        assertEquals(12L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(orderDetailMapper, never()).getByOrderIds(any());
        verify(orderDetailMapper, never()).getByOrderId(any());
    }

    /**
     * 创建包含指定订单的分页结果
     *
     * @param pageNum 当前页码
     * @param pageSize 每页订单数
     * @param total 符合条件的订单总数
     * @param orders 当前页订单
     * @return PageHelper 分页对象
     */
    private Page<Orders> page(int pageNum, int pageSize, long total, Orders... orders) {
        Page<Orders> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.addAll(Arrays.asList(orders));
        return page;
    }

    /**
     * 创建指定订单编号的订单
     *
     * @param id 订单编号
     * @return 测试订单
     */
    private Orders order(Long id) {
        return Orders.builder().id(id).number("ORDER-" + id).build();
    }

    /**
     * 创建关联指定订单的明细
     *
     * @param id 明细编号
     * @param orderId 所属订单编号
     * @return 测试明细
     */
    private OrderDetail detail(Long id, Long orderId) {
        return OrderDetail.builder().id(id).orderId(orderId).name("Dish " + id).build();
    }
}
