package com.sky.controller.user;

import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.service.DishService;
import com.sky.service.MenuCacheService;
import com.sky.service.OrderService;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuCacheFailureTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private DishService dishService;
    private SetmealService setmealService;
    private OrderService orderService;
    private MenuCacheService menuCacheService;

    /**
     * 初始化用户菜单接口及其 Redis 和业务依赖
     */
    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        menuCacheService = new MenuCacheService();
        ReflectionTestUtils.setField(menuCacheService, "redisTemplate", redisTemplate);
        dishService = mock(DishService.class);
        setmealService = mock(SetmealService.class);
        orderService = mock(OrderService.class);
        when(orderService.getDishMonthlySales(anyList())).thenReturn(Collections.emptyMap());
        when(orderService.getSetmealMonthlySales(anyList())).thenReturn(Collections.emptyMap());
    }

    /**
     * 验证 Redis 连接异常时菜品和套餐接口均回源业务查询并返回数据
     */
    @Test
    void shouldLoadBothMenusFromDatabaseAfterCacheReadFailure() {
        when(valueOperations.get(anyString()))
            .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        when(redisTemplate.delete(anyString()))
            .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        List<DishVO> dishes = Collections.singletonList(DishVO.builder().id(1L).build());
        List<Setmeal> setmeals = Collections.singletonList(Setmeal.builder().id(2L).build());
        when(dishService.listWithFlavor(any(Dish.class))).thenReturn(dishes);
        when(setmealService.list(any(Setmeal.class))).thenReturn(setmeals);
        DishController dishController = new DishController();
        SetmealController setmealController = new SetmealController();
        ReflectionTestUtils.setField(dishController, "dishService", dishService);
        ReflectionTestUtils.setField(dishController, "menuCacheService", menuCacheService);
        ReflectionTestUtils.setField(dishController, "orderService", orderService);
        ReflectionTestUtils.setField(setmealController, "setmealService", setmealService);
        ReflectionTestUtils.setField(setmealController, "menuCacheService", menuCacheService);
        ReflectionTestUtils.setField(setmealController, "orderService", orderService);

        assertEquals(dishes, dishController.list(10L).getData());
        assertEquals(setmeals, setmealController.list(20L).getData());

        verify(dishService).listWithFlavor(any(Dish.class));
        verify(setmealService).list(any(Setmeal.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * 验证数据库回源成功后缓存写入失败不影响菜品和套餐接口返回
     */
    @Test
    void shouldFailBothMenuRequestsWhenCacheWriteFailsAfterDatabaseRead() {
        when(valueOperations.get(anyString())).thenReturn(null);
        doThrow(new RedisConnectionFailureException("Redis unavailable"))
            .when(valueOperations).set(anyString(), any(), any());
        when(dishService.listWithFlavor(any(Dish.class))).thenReturn(Collections.emptyList());
        when(setmealService.list(any(Setmeal.class))).thenReturn(Collections.emptyList());
        DishController dishController = new DishController();
        SetmealController setmealController = new SetmealController();
        ReflectionTestUtils.setField(dishController, "dishService", dishService);
        ReflectionTestUtils.setField(dishController, "menuCacheService", menuCacheService);
        ReflectionTestUtils.setField(dishController, "orderService", orderService);
        ReflectionTestUtils.setField(setmealController, "setmealService", setmealService);
        ReflectionTestUtils.setField(setmealController, "menuCacheService", menuCacheService);
        ReflectionTestUtils.setField(setmealController, "orderService", orderService);

        assertEquals(Collections.emptyList(), dishController.list(10L).getData());
        assertEquals(Collections.emptyList(), setmealController.list(20L).getData());

        verify(dishService).listWithFlavor(any(Dish.class));
        verify(setmealService).list(any(Setmeal.class));
    }
}
