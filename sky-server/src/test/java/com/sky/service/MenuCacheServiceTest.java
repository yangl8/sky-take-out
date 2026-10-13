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
     * Initializes the menu cache service and its Redis dependency
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
     * Verifies read failures return a cache miss after successful eviction
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
     * Verifies cache hits return the cached dish or set-meal data
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
     * Verifies Redis connection failures do not trigger futile stale-key deletion requests
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
     * Verifies non-connection read failures attempt eviction once and still allow a database fallback
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
     * Verifies cache write failures do not affect callers and explicit eviction uses bounded retries
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
     * Verifies transactional eviction waits for commit and retries deletion a bounded number of times
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
     * Verifies rollback does not evict dish or set-meal caches
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
     * Verifies persistent post-commit eviction failures are not propagated to callers of the committed operation
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
     * Verifies eviction runs immediately when no transaction is active
     */
    @Test
    void shouldEvictImmediatelyWithoutTransaction() {
        menuCacheService.evictDishes(10L);
        menuCacheService.evictSetmeals(20L);

        verify(redisTemplate).delete("menu:dish:category:10");
        verify(redisTemplate).delete("menu:setmeal:category:20");
    }

    /**
     * Verifies regular and empty-result menu caches both have bounded lifetimes
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
