package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.config.DeliveryTimeProperties;
import com.sky.constant.ShopStatusConstant;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Dish;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.DeliveryTimeValidationService;
import com.sky.utils.WeChatPayUtil;
import com.sky.websocket.WebSocketServer;
import com.sky.vo.OrderSubmitVO;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单服务端商品计价测试
 */
@ExtendWith(MockitoExtension.class)
class OrderPricingTest {

    private static final Long USER_ID = 7L;
    private static final Long ADDRESS_ID = 10L;

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
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @Spy
    @InjectMocks
    private OrderServiceImpl orderService;

    /**
     * 初始化可控的下单依赖
     */
    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(USER_ID);
        ReflectionTestUtils.setField(orderService, "deliveryFee", new BigDecimal("2.50"));
        DeliveryTimeProperties properties = new DeliveryTimeProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-10-10T02:00:00Z"), ZoneOffset.UTC);
        ReflectionTestUtils.setField(orderService, "deliveryTimeProperties", properties);
        ReflectionTestUtils.setField(orderService, "clock", clock);
        ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService",
                new DeliveryTimeValidationService(properties, clock));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(ShopStatusConstant.KEY)).thenReturn(1);
        doNothing().when(orderService).checkOutOfRange(any(AddressBook.class));
        when(addressBookMapper.getByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(AddressBook.builder()
                .id(ADDRESS_ID)
                .userId(USER_ID)
                .provinceName("Province")
                .cityName("City")
                .districtName("District")
                .detail("Street 1")
                .phone("0600000000")
                .consignee("Test User")
                .build());
    }

    /**
     * 清理测试线程中的登录身份
     */
    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    /**
     * 按当前菜品和套餐价格计价并保存订单商品快照
     */
    @Test
    void submitOrderShouldIgnoreClientAmountAndUseCurrentCatalogPrices() {
        ShoppingCart dishCartItem = cartItem(100L, null, 2, "1.00");
        ShoppingCart setmealCartItem = cartItem(null, 200L, 1, "99.00");
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(Arrays.asList(dishCartItem, setmealCartItem));
        when(dishMapper.getById(100L)).thenReturn(Dish.builder()
                .id(100L).name("Current dish name").image("current-dish.png")
                .price(new BigDecimal("3.50")).status(1).build());
        when(setmealMapper.getById(200L)).thenReturn(Setmeal.builder()
                .id(200L).name("Current setmeal name").image("current-setmeal.png")
                .price(new BigDecimal("8.25")).status(1).build());
        doAnswer(invocation -> {
            invocation.<Orders>getArgument(0).setId(500L);
            return null;
        }).when(orderMapper).insert(any(Orders.class));

        OrdersSubmitDTO request = new OrdersSubmitDTO();
        request.setAddressBookId(ADDRESS_ID);
        request.setDeliveryStatus(1);
        request.setTablewareNumber(3);
        request.setTablewareStatus(0);
        request.setAmount(new BigDecimal("0.01"));
        request.setPackAmount(999);

        OrderSubmitVO response = orderService.submitOrder(request);

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Orders savedOrder = orderCaptor.getValue();
        assertEquals(new BigDecimal("20.75"), savedOrder.getAmount());
        assertEquals(3, savedOrder.getPackAmount());
        assertEquals(new BigDecimal("20.75"), response.getOrderAmount());

        ArgumentCaptor<List> detailsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderDetailMapper).insertBatch(detailsCaptor.capture());
        List<OrderDetail> savedDetails = detailsCaptor.getValue();
        assertEquals(2, savedDetails.size());
        assertEquals("Current dish name", savedDetails.get(0).getName());
        assertEquals("current-dish.png", savedDetails.get(0).getImage());
        assertEquals(new BigDecimal("3.50"), savedDetails.get(0).getAmount());
        assertEquals(2, savedDetails.get(0).getNumber());
        assertEquals(500L, savedDetails.get(0).getOrderId());
        assertEquals("Current setmeal name", savedDetails.get(1).getName());
        assertEquals(new BigDecimal("8.25"), savedDetails.get(1).getAmount());
        verify(shoppingCartMapper).deleteByUserId(USER_ID);
    }

    @Test
    void submitOrderTimeUsesInjectedClockInNonBeijingJvmZone() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        ZoneId jvmZone = ZoneId.of("America/Los_Angeles");
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(jvmZone));
            Clock fixedClock = Clock.fixed(Instant.parse("2026-10-10T02:00:00Z"), ZoneOffset.UTC);
            DeliveryTimeProperties properties = new DeliveryTimeProperties();
            ReflectionTestUtils.setField(orderService, "deliveryTimeProperties", properties);
            ReflectionTestUtils.setField(orderService, "clock", fixedClock);
            ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService",
                    new DeliveryTimeValidationService(properties, fixedClock));
            prepareSuccessfulDishOrder();

            orderService.submitOrder(request());

            ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
            verify(orderMapper).insert(orderCaptor.capture());
            assertEquals(LocalDateTime.ofInstant(fixedClock.instant(), jvmZone), orderCaptor.getValue().getOrderTime());
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    /**
     * 商品数量为空、零或负数时拒绝下单
     *
     * @param quantity 购物车中的商品数量
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void submitOrderShouldRejectInvalidQuantities(Integer quantity) {
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(Arrays.asList(cartItem(100L, null, quantity, "3.50")));

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(request()));

        verifyNoOrderWasCreated();
    }

    /**
     * 购物车商品对应的菜品不存在时拒绝下单
     */
    @Test
    void submitOrderShouldRejectMissingDish() {
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(Arrays.asList(cartItem(100L, null, 1, "3.50")));
        when(dishMapper.getById(100L)).thenReturn(null);

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(request()));

        verifyNoOrderWasCreated();
    }

    /**
     * 菜品停售后拒绝使用购物车中的旧商品下单
     */
    @Test
    void submitOrderShouldRejectDisabledDish() {
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(Arrays.asList(cartItem(100L, null, 1, "3.50")));
        when(dishMapper.getById(100L)).thenReturn(Dish.builder()
                .id(100L).name("Dish").price(new BigDecimal("3.50")).status(0).build());

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(request()));

        verifyNoOrderWasCreated();
    }

    /**
     * 套餐停售后拒绝使用购物车中的旧套餐下单
     */
    @Test
    void submitOrderShouldRejectDisabledSetmeal() {
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(Arrays.asList(cartItem(null, 200L, 1, "8.25")));
        when(setmealMapper.getById(200L)).thenReturn(Setmeal.builder()
                .id(200L).name("Setmeal").price(new BigDecimal("8.25")).status(0).build());

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(request()));

        verifyNoOrderWasCreated();
    }

    /**
     * 当前商品价格为空或负数时拒绝下单
     *
     * @param currentPrice 商品当前价格
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.01"})
    void submitOrderShouldRejectInvalidCurrentPrice(String currentPrice) {
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(Arrays.asList(cartItem(100L, null, 1, "3.50")));
        when(dishMapper.getById(100L)).thenReturn(Dish.builder()
                .id(100L).name("Dish").price(currentPrice == null ? null : new BigDecimal(currentPrice))
                .status(1).build());

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(request()));

        verifyNoOrderWasCreated();
    }

    /**
     * 购物车行未且仅未关联一种商品时拒绝下单
     */
    @Test
    void submitOrderShouldRejectCartRowWithoutProductReference() {
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Arrays.asList(
                ShoppingCart.builder().number(1).build()));

        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(request()));

        verifyNoOrderWasCreated();
    }

    /**
     * 构造用于测试的购物车商品
     *
     * @param dishId 菜品ID
     * @param setmealId 套餐ID
     * @param quantity 商品数量
     * @param staleAmount 购物车中可能过期的单价
     * @return 购物车商品
     */
    private ShoppingCart cartItem(Long dishId, Long setmealId, Integer quantity, String staleAmount) {
        return ShoppingCart.builder()
                .dishId(dishId)
                .setmealId(setmealId)
                .number(quantity)
                .amount(new BigDecimal(staleAmount))
                .name("Stale cart name")
                .image("stale-cart.png")
                .dishFlavor("spicy")
                .build();
    }

    private void prepareSuccessfulDishOrder() {
        when(shoppingCartMapper.list(any(ShoppingCart.class)))
                .thenReturn(Arrays.asList(cartItem(100L, null, 1, "3.50")));
        when(dishMapper.getById(100L)).thenReturn(Dish.builder().id(100L).name("Dish")
                .image("dish.png").price(new BigDecimal("3.50")).status(1).build());
        doAnswer(invocation -> {
            invocation.<Orders>getArgument(0).setId(500L);
            return null;
        }).when(orderMapper).insert(any(Orders.class));
    }

    /**
     * 构造测试使用的订单请求
     *
     * @return 订单请求
     */
    private OrdersSubmitDTO request() {
        OrdersSubmitDTO request = new OrdersSubmitDTO();
        request.setAddressBookId(ADDRESS_ID);
        request.setDeliveryStatus(1);
        request.setTablewareNumber(0);
        request.setTablewareStatus(0);
        request.setAmount(new BigDecimal("999.99"));
        return request;
    }

    /**
     * 确认失败下单没有写入订单、订单明细或清空购物车
     */
    private void verifyNoOrderWasCreated() {
        verify(orderMapper, never()).insert(any(Orders.class));
        verify(orderDetailMapper, never()).insertBatch(any());
        verify(shoppingCartMapper, never()).deleteByUserId(USER_ID);
    }
}
