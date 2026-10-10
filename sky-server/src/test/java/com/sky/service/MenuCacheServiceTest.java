package com.sky.service;

import com.sky.entity.Setmeal;
import com.sky.vo.DishVO;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        when(valueOperations.get(anyString())).thenReturn("stale incompatible value");
        when(redisTemplate.delete(anyString())).thenReturn(true);

        assertNull(menuCacheService.getDishes(10L));
        assertNull(menuCacheService.getSetmeals(20L));

        verify(redisTemplate).delete("menu:dish:category:10");
        verify(redisTemplate).delete("menu:setmeal:category:20");
    }

    /**
     * 验证菜品和套餐缓存命中时直接返回缓存对象
     */
    @Test
    void shouldReturnCachedMenusWithoutDeletingKeys() {
        List<DishVO> dishes = Collections.singletonList(DishVO.builder().id(1L).build());
        List<Setmeal> setmeals = Collections.singletonList(Setmeal.builder().id(2L).build());
        when(valueOperations.get("menu:dish:category:10")).thenReturn(dishes);
        when(valueOperations.get("menu:setmeal:category:20")).thenReturn(setmeals);

        assertSame(dishes, menuCacheService.getDishes(10L));
        assertSame(setmeals, menuCacheService.getSetmeals(20L));

        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * 验证 Redis 连接故障时不再执行无效的旧键删除请求
     */
    @Test
    void shouldSkipBrokenKeyDeletionWhenRedisConnectionFails() {
        when(valueOperations.get(anyString()))
            .thenThrow(new RedisConnectionFailureException("Redis unavailable"));
        when(redisTemplate.delete(anyString()))
            .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

        assertNull(menuCacheService.getDishes(10L));
        assertNull(menuCacheService.getSetmeals(20L));

        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * 验证非连接型缓存读取异常的删除失败只尝试一次且不阻断回源
     */
    @Test
    void shouldAttemptBrokenKeyDeletionOnlyOnce() {
        when(valueOperations.get(anyString())).thenReturn("stale incompatible value");
        when(redisTemplate.delete(anyString()))
            .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

        assertNull(menuCacheService.getDishes(10L));
        assertNull(menuCacheService.getSetmeals(20L));

        verify(redisTemplate).delete("menu:dish:category:10");
        verify(redisTemplate).delete("menu:setmeal:category:20");
    }

    /**
     * 验证缓存写入失败不影响调用方且主动失效执行有限重试
     */
    @Test
    void shouldNotPropagateWriteFailureAndBoundEvictionRetries() {
        RedisConnectionFailureException failure = new RedisConnectionFailureException("Redis unavailable");
        doThrow(failure).when(valueOperations).set(anyString(), any(), any());
        doThrow(failure).when(redisTemplate).delete(anyString());
        List<DishVO> dishes = Collections.emptyList();
        List<Setmeal> setmeals = Collections.emptyList();

        menuCacheService.cacheDishes(10L, dishes);
        menuCacheService.cacheSetmeals(20L, setmeals);
        menuCacheService.evictDishes(10L);
        menuCacheService.evictSetmeals(20L);

        verify(valueOperations, times(2)).set(anyString(), any(), any());
        verify(redisTemplate, times(6)).delete(anyString());
    }

    /**
     * 验证事务中的缓存失效延迟到提交后并对删除进行有限重试
     */
    @Test
    void shouldEvictOnlyAfterCommitAndRetryTransientFailure() {
        RedisConnectionFailureException failure = new RedisConnectionFailureException("Redis unavailable");
        doThrow(failure).doThrow(failure).doReturn(true)
            .when(redisTemplate).delete(eq("menu:dish:category:10"));
        doReturn(true).when(redisTemplate).delete(eq("menu:setmeal:category:20"));
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            menuCacheService.evictDishes(10L);
            menuCacheService.evictSetmeals(20L);

            verify(redisTemplate, never()).delete(anyString());
            TransactionSynchronizationUtils.triggerAfterCommit();

            verify(redisTemplate, times(3)).delete("menu:dish:category:10");
            verify(redisTemplate).delete("menu:setmeal:category:20");
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * 验证事务回滚后不执行菜品或套餐缓存失效
     */
    @Test
    void shouldNotEvictWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            menuCacheService.evictDishes(10L);
            menuCacheService.evictSetmeals(20L);
            TransactionSynchronizationUtils.triggerAfterCompletion(
                org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(redisTemplate, never()).delete(anyString());
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * 验证事务提交后删除持续失败不会向已提交操作的调用方传播
     */
    @Test
    void shouldNotPropagatePersistentEvictionFailureAfterCommit() {
        RedisConnectionFailureException failure = new RedisConnectionFailureException("Redis unavailable");
        doThrow(failure).when(redisTemplate).delete("menu:dish:category:10");
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            menuCacheService.evictDishes(10L);

            assertDoesNotThrow(TransactionSynchronizationUtils::triggerAfterCommit);
            verify(redisTemplate, times(3)).delete("menu:dish:category:10");
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * 验证没有活动事务时立即执行缓存失效
     */
    @Test
    void shouldEvictImmediatelyWithoutTransaction() {
        menuCacheService.evictDishes(10L);
        menuCacheService.evictSetmeals(20L);

        verify(redisTemplate).delete("menu:dish:category:10");
        verify(redisTemplate).delete("menu:setmeal:category:20");
    }

    /**
     * 验证普通菜单缓存和空结果缓存的有效期均有明确上限
     */
    @Test
    void shouldKeepCacheExpiryWithinConfiguredBounds() {
        ArgumentCaptor<Duration> expirationCaptor = ArgumentCaptor.forClass(Duration.class);

        menuCacheService.cacheDishes(10L, Collections.singletonList(DishVO.builder().id(1L).build()));
        menuCacheService.cacheSetmeals(20L, Collections.emptyList());

        verify(valueOperations).set(eq("menu:dish:category:10"), any(), expirationCaptor.capture());
        Duration populatedExpiration = expirationCaptor.getValue();
        assertTrue(populatedExpiration.compareTo(Duration.ofMinutes(5)) >= 0);
        assertTrue(populatedExpiration.compareTo(Duration.ofMinutes(10)) <= 0);

        verify(valueOperations).set(eq("menu:setmeal:category:20"), any(), expirationCaptor.capture());
        Duration emptyExpiration = expirationCaptor.getValue();
        assertTrue(emptyExpiration.compareTo(Duration.ofMinutes(2)) >= 0);
        assertTrue(emptyExpiration.compareTo(Duration.ofMinutes(5)) <= 0);
    }
}
