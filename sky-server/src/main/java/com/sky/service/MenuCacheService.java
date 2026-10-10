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
 * 菜单缓存服务，统一管理缓存键、过期时间和Redis操作
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
            log.warn("菜品缓存读取失败，按未命中处理：{}", key, exception);
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
            log.warn("套餐缓存读取失败，按未命中处理：{}", key, exception);
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
            log.warn("菜单缓存写入失败，不影响数据库查询结果：{}", key, exception);
        }
    }

    /**
     * 在当前事务提交后失效缓存，避免提交前的并发回源重新写入旧数据
     *
     * @param key 菜品或套餐缓存键
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
     * 读取异常时仅清理非连接故障导致的旧值，避免 Redis 宕机期间重复请求
     *
     * @param key 待读取的缓存键
     * @param failure 缓存读取异常
     */
    private void deleteAfterReadFailure(String key, RuntimeException failure) {
        if (isConnectionFailure(failure)) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException exception) {
            log.warn("读取失败后的菜单缓存清理失败，继续按未命中处理：{}", key, exception);
        }
    }

    /**
     * 判断缓存异常链中是否包含 Redis 连接故障
     *
     * @param failure 缓存操作异常
     * @return 异常链包含 Redis 连接故障时返回 true
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
     * 有限重试删除缓存，持续失败时记录错误并依靠短 TTL 限制旧数据存活时间
     *
     * @param key 待删除的缓存键
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
        log.error("菜单缓存失效在 {} 次尝试后仍失败，旧数据最多保留至 TTL 到期：{}",
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
