package com.sky.service;

import com.sky.entity.Setmeal;
import com.sky.vo.DishVO;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuCacheServiceTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private MenuCacheService menuCacheService;

    /**
     * 初始化菜单缓存服务及其 Redis 依赖
     */
    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        menuCacheService = new MenuCacheService();
        ReflectionTestUtils.setField(menuCacheService, "redisTemplate", redisTemplate);
    }

    /**
     * 验证菜品和套餐缓存读取失败后删除成功时返回缓存未命中
     */
    @Test
    void shouldReturnCacheMissWhenReadFailsAndBrokenEntriesAreDeleted() {
        when(valueOperations.get(anyString()))
            .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        assertNull(menuCacheService.getDishes(10L));
        assertNull(menuCacheService.getSetmeals(20L));

        verify(redisTemplate).delete("menu:dish:category:10");
        verify(redisTemplate).delete("menu:setmeal:category:20");
    }

    /**
     * 验证读取失败后的缓存删除异常会继续向调用方传播
     */
    @Test
    void shouldPropagateDeleteFailureAfterReadFailure() {
        when(valueOperations.get(anyString()))
            .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        when(redisTemplate.delete(anyString()))
            .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

        assertThrows(RedisConnectionFailureException.class, () -> menuCacheService.getDishes(10L));
        assertThrows(RedisConnectionFailureException.class, () -> menuCacheService.getSetmeals(20L));
    }

    /**
     * 验证菜品和套餐缓存写入及主动失效异常会向调用方传播
     */
    @Test
    void shouldPropagateCacheWriteAndEvictionFailures() {
        RedisConnectionFailureException failure = new RedisConnectionFailureException("Redis unavailable");
        doThrow(failure).when(valueOperations).set(anyString(), any(), any());
        doThrow(failure).when(redisTemplate).delete(anyString());
        List<DishVO> dishes = Collections.emptyList();
        List<Setmeal> setmeals = Collections.emptyList();

        assertThrows(RedisConnectionFailureException.class, () -> menuCacheService.cacheDishes(10L, dishes));
        assertThrows(RedisConnectionFailureException.class, () -> menuCacheService.cacheSetmeals(20L, setmeals));
        assertThrows(RedisConnectionFailureException.class, () -> menuCacheService.evictDishes(10L));
        assertThrows(RedisConnectionFailureException.class, () -> menuCacheService.evictSetmeals(20L));
    }
}
