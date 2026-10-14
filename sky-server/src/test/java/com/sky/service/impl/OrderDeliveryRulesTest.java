package com.sky.service.impl;

import com.sky.config.DeliveryTimeProperties;
import com.sky.config.RedisConfiguration;
import com.sky.constant.ShopStatusConstant;
import com.sky.controller.admin.ShopController;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.DeliveryTimeValidationService;
import com.sky.utils.WeChatPayUtil;
import com.sky.websocket.WebSocketServer;
import com.sky.vo.OrderSubmitVO;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryRulesTest {

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

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(USER_ID);
        ReflectionTestUtils.setField(orderService, "deliveryFee", BigDecimal.ZERO);
        ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService", validationService("2026-10-10T02:00:00Z"));
        when(addressBookMapper.getByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(AddressBook.builder()
                .id(ADDRESS_ID).userId(USER_ID).cityName("City").districtName("District")
                .detail("Street 1").phone("0600000000").consignee("Test User").build());
        lenient().doNothing().when(orderService).checkOutOfRange(any(AddressBook.class));
    }

    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    @Test
    void immediateOrderCanBeCreatedDuringBusinessHoursWhenRedisIsOpen() {
        mockShopStatus(1);
        prepareSuccessfulOrder();
        OrdersSubmitDTO request = request(1, null);

        OrderSubmitVO response = orderService.submitOrder(request);

        assertEquals(500L, response.getId());
        verify(redisTemplate.opsForValue()).get(ShopStatusConstant.KEY);
        verify(orderMapper).insert(any(Orders.class));
        verify(orderDetailMapper).insertBatch(any());
        verify(shoppingCartMapper).deleteByUserId(USER_ID);
    }

    @Test
    void adminWrittenIntegerOpenStatusUsesConfiguredSerializerAndAllowsSubmission() {
        RedisTemplate configuredTemplate = configuredRedisTemplate();
        assertEquals(StringRedisSerializer.class, configuredTemplate.getKeySerializer().getClass());
        assertEquals(JdkSerializationRedisSerializer.class, configuredTemplate.getValueSerializer().getClass());
        AtomicReference<byte[]> storedValue = connectControllerAndOrderServiceThroughSerializers(configuredTemplate);
        ShopController adminShopController = adminShopController();
        prepareSuccessfulOrder();

        adminShopController.setStatus(1);
        Object valueReadBack = configuredTemplate.getValueSerializer().deserialize(storedValue.get());
        assertInstanceOf(Integer.class, valueReadBack);
        assertEquals(Integer.valueOf(1), valueReadBack);

        OrderSubmitVO response = orderService.submitOrder(request(1, null));

        assertEquals(500L, response.getId());
        verify(orderMapper).insert(any(Orders.class));
        assertEquals(ShopStatusConstant.KEY, ShopController.KEY);
        assertEquals(ShopStatusConstant.KEY, com.sky.controller.user.ShopController.KEY);
    }

    @Test
    void adminWrittenIntegerClosedStatusIsRejectedBySubmission() {
        RedisTemplate configuredTemplate = configuredRedisTemplate();
        AtomicReference<byte[]> storedValue = connectControllerAndOrderServiceThroughSerializers(configuredTemplate);
        ShopController adminShopController = adminShopController();

        adminShopController.setStatus(0);
        Object valueReadBack = configuredTemplate.getValueSerializer().deserialize(storedValue.get());
        assertInstanceOf(Integer.class, valueReadBack);
        assertEquals(Integer.valueOf(0), valueReadBack);

        assertRejectedWithoutOrderSideEffects(request(1, null));
    }

    @Test
    void immediateOrderIsRejectedWhenRedisSaysShopIsClosed() {
        mockShopStatus(0);

        assertRejectedWithoutOrderSideEffects(request(1, null));
    }

    @Test
    void immediateOrderIsRejectedWhenRedisStatusIsMissingOrInvalid() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(ShopStatusConstant.KEY)).thenReturn(null, "1");
        assertRejectedWithoutOrderSideEffects(request(1, null));
        assertRejectedWithoutOrderSideEffects(request(1, null));
    }

    @Test
    void immediateOrderIsRejectedWhenRedisReadFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(ShopStatusConstant.KEY)).thenThrow(new IllegalStateException("Redis unavailable"));

        assertRejectedWithoutOrderSideEffects(request(1, null));
    }

    @Test
    void immediateOrderIsRejectedAfterClosingEvenWhenRedisStillSaysOpen() {
        ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService", validationService("2026-10-10T14:00:01Z"));
        mockShopStatus(1);

        assertRejectedWithoutOrderSideEffects(request(1, null));

        verify(valueOperations, never()).get(ShopStatusConstant.KEY);
    }

    @Test
    void immediateOrderMayBeAcceptedBeforeClosingEvenWhenEstimatedDeliveryIsAfterClosing() {
        ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService", validationService("2026-10-10T13:50:00Z"));
        mockShopStatus(1);
        prepareSuccessfulOrder();

        OrderSubmitVO response = orderService.submitOrder(request(1, java.time.LocalDateTime.of(2026, 10, 10, 22, 20)));

        assertEquals(500L, response.getId());
        verify(orderMapper).insert(any(Orders.class));
    }

    @Test
    void scheduledOrderCanBeCreatedWhileShopIsClosedWhenAppointmentIsValid() {
        ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService", validationService("2026-10-10T14:30:00Z"));
        prepareSuccessfulOrder();

        OrderSubmitVO response = orderService.submitOrder(request(0, java.time.LocalDateTime.of(2026, 10, 11, 12, 0)));

        assertEquals(500L, response.getId());
        verify(redisTemplate, never()).opsForValue();
        verify(orderMapper).insert(any(Orders.class));
    }

    @Test
    void invalidAppointmentAndDeliveryTypeAreRejectedBeforeOrderSideEffects() {
        assertRejectedWithoutOrderSideEffects(request(0, null));
        assertRejectedWithoutOrderSideEffects(request(0, java.time.LocalDateTime.of(2026, 10, 10, 10, 29)));
        assertRejectedWithoutOrderSideEffects(request(0, java.time.LocalDateTime.of(2026, 10, 10, 22, 1)));
        assertRejectedWithoutOrderSideEffects(request(2, java.time.LocalDateTime.of(2026, 10, 10, 12, 0)));
        assertRejectedWithoutOrderSideEffects(request(null, null));

        ReflectionTestUtils.setField(orderService, "deliveryTimeValidationService", validationService("2026-10-10T02:45:00Z"));
        assertRejectedWithoutOrderSideEffects(request(0, java.time.LocalDateTime.of(2026, 10, 10, 11, 0)));
    }

    private void mockShopStatus(Object status) {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(ShopStatusConstant.KEY)).thenReturn(status);
    }

    private RedisTemplate configuredRedisTemplate() {
        RedisTemplate template = new RedisConfiguration().redisTemplate(mock(RedisConnectionFactory.class));
        template.afterPropertiesSet();
        return template;
    }

    private AtomicReference<byte[]> connectControllerAndOrderServiceThroughSerializers(RedisTemplate configuredTemplate) {
        AtomicReference<byte[]> storedValue = new AtomicReference<>();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().doAnswer(invocation -> {
            Object value = invocation.getArgument(1);
            storedValue.set((byte[]) configuredTemplate.getValueSerializer().serialize(value));
            return null;
        }).when(valueOperations).set(anyString(), any());
        lenient().when(valueOperations.get(ShopStatusConstant.KEY)).thenAnswer(invocation ->
                configuredTemplate.getValueSerializer().deserialize(storedValue.get()));
        return storedValue;
    }

    private ShopController adminShopController() {
        ShopController controller = new ShopController();
        ReflectionTestUtils.setField(controller, "redisTemplate", redisTemplate);
        return controller;
    }

    private void prepareSuccessfulOrder() {
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Collections.singletonList(
                ShoppingCart.builder().userId(USER_ID).dishId(100L).number(1).build()));
        when(dishMapper.getById(100L)).thenReturn(Dish.builder()
                .id(100L).name("Dish").image("dish.png").price(BigDecimal.ONE).status(1).build());
        doAnswer(invocation -> {
            invocation.<Orders>getArgument(0).setId(500L);
            return null;
        }).when(orderMapper).insert(any(Orders.class));
    }

    private OrdersSubmitDTO request(Integer deliveryStatus, java.time.LocalDateTime estimatedDeliveryTime) {
        OrdersSubmitDTO request = new OrdersSubmitDTO();
        request.setAddressBookId(ADDRESS_ID);
        request.setDeliveryStatus(deliveryStatus);
        request.setEstimatedDeliveryTime(estimatedDeliveryTime);
        request.setTablewareNumber(0);
        request.setTablewareStatus(0);
        return request;
    }

    private DeliveryTimeValidationService validationService(String instant) {
        DeliveryTimeProperties properties = new DeliveryTimeProperties();
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        ReflectionTestUtils.setField(orderService, "deliveryTimeProperties", properties);
        ReflectionTestUtils.setField(orderService, "clock", clock);
        return new DeliveryTimeValidationService(properties, clock);
    }

    private void assertRejectedWithoutOrderSideEffects(OrdersSubmitDTO request) {
        assertThrows(OrderBusinessException.class, () -> orderService.submitOrder(request));
        verify(orderMapper, never()).insert(any(Orders.class));
        verify(orderDetailMapper, never()).insertBatch(any());
        verify(shoppingCartMapper, never()).deleteByUserId(USER_ID);
    }
}
