package com.sky.service;

import com.sky.constant.CacheKeyConstant;
import com.sky.entity.Setmeal;
import com.sky.vo.DishVO;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Centralizes menu cache keys, expiration, and Redis operations
 */
@Service
@Slf4j
public class MenuCacheService {

    private static final int DATA_TTL_MINUTES = 5;
    private static final int DATA_TTL_JITTER_MINUTES = 5;
    private static final int EMPTY_TTL_MINUTES = 2;
    private static final int EMPTY_TTL_JITTER_MINUTES = 3;
    private static final int EVICTION_ATTEMPTS = 3;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<DishVO> getDishes(Long categoryId) {
        String key = dishKey(categoryId);
        try {
            return (List<DishVO>) redisTemplate.opsForValue().get(key);
        } catch (RuntimeException exception) {
            log.warn("Failed to read dish cache; treating it as a cache miss: {}", key, exception);
            deleteAfterReadFailure(key, exception);
            return null;
        }
    }

    public void cacheDishes(Long categoryId, List<DishVO> dishes) {
        cache(dishKey(categoryId), dishes);
    }

    @SuppressWarnings("unchecked")
    public List<Setmeal> getSetmeals(Long categoryId) {
        String key = setmealKey(categoryId);
        try {
            return (List<Setmeal>) redisTemplate.opsForValue().get(key);
        } catch (RuntimeException exception) {
            log.warn("Failed to read set meal cache; treating it as a cache miss: {}", key, exception);
            deleteAfterReadFailure(key, exception);
            return null;
        }
    }

    public void cacheSetmeals(Long categoryId, List<Setmeal> setmeals) {
        cache(setmealKey(categoryId), setmeals);
    }

    public void evictDishes(Long categoryId) {
        evictAfterCommit(dishKey(categoryId));
    }

    public void evictSetmeals(Long categoryId) {
        evictAfterCommit(setmealKey(categoryId));
    }

    private void cache(String key, List<?> values) {
        try {
            redisTemplate.opsForValue().set(key, values, expiration(values == null || values.isEmpty()));
        } catch (RuntimeException exception) {
            log.warn("Failed to write menu cache; database results remain available: {}", key, exception);
        }
    }

    /**
     * Evicts the cache after commit so a concurrent database read cannot repopulate stale data before commit
     *
     * @param key Dish or set-meal cache key
     */
    private void evictAfterCommit(String key) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
            && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteWithRetry(key);
                }
            });
            return;
        }
        deleteWithRetry(key);
    }

    /**
     * Clears stale values after non-connection read failures; connection outages should not trigger repeated delete attempts
     *
     * @param key Cache key that failed to read
     * @param failure Cache read failure
     */
    private void deleteAfterReadFailure(String key, RuntimeException failure) {
        if (isConnectionFailure(failure)) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException exception) {
            log.warn("Failed to clear menu cache after a read error; continuing with a cache miss: {}", key, exception);
        }
    }

    /**
     * Checks whether the exception chain contains a Redis connection failure
     *
     * @param failure Cache operation failure
     * @return {@code true} when the cause chain contains a Redis connection failure
     */
    private boolean isConnectionFailure(RuntimeException failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof RedisConnectionFailureException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Retries eviction a bounded number of times; short TTLs limit stale data if Redis remains unavailable
     *
     * @param key Cache key to evict
     */
    private void deleteWithRetry(String key) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= EVICTION_ATTEMPTS; attempt++) {
            try {
                redisTemplate.delete(key);
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        log.error("Menu cache eviction failed after {} attempts; stale data may remain until its TTL expires: {}",
            EVICTION_ATTEMPTS, key, lastFailure);
    }

    private Duration expiration(boolean empty) {
        int minimum = empty ? EMPTY_TTL_MINUTES : DATA_TTL_MINUTES;
        int jitter = empty ? EMPTY_TTL_JITTER_MINUTES : DATA_TTL_JITTER_MINUTES;
        return Duration.ofMinutes(minimum + ThreadLocalRandom.current().nextInt(jitter + 1));
    }

    private String dishKey(Long categoryId) {
        return CacheKeyConstant.DISH_CATEGORY_PREFIX + categoryId;
    }

    private String setmealKey(Long categoryId) {
        return CacheKeyConstant.SETMEAL_CATEGORY_PREFIX + categoryId;
    }

}
