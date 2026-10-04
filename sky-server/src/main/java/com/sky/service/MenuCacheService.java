package com.sky.service;

import com.sky.constant.CacheKeyConstant;
import com.sky.entity.Setmeal;
import com.sky.vo.DishVO;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

/**
 * Centralizes Redis keys, expiration rules and operations for menu caches.
 */
@Service
public class MenuCacheService {

    private static final int DATA_TTL_MINUTES = 60;
    private static final int DATA_TTL_JITTER_MINUTES = 30;
    private static final int EMPTY_TTL_MINUTES = 2;
    private static final int EMPTY_TTL_JITTER_MINUTES = 3;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<DishVO> getDishes(Long categoryId) {
        return (List<DishVO>) redisTemplate.opsForValue().get(dishKey(categoryId));
    }

    public void cacheDishes(Long categoryId, List<DishVO> dishes) {
        cache(dishKey(categoryId), dishes);
    }

    @SuppressWarnings("unchecked")
    public List<Setmeal> getSetmeals(Long categoryId) {
        return (List<Setmeal>) redisTemplate.opsForValue().get(setmealKey(categoryId));
    }

    public void cacheSetmeals(Long categoryId, List<Setmeal> setmeals) {
        cache(setmealKey(categoryId), setmeals);
    }

    public void evictDishes(Long categoryId) {
        redisTemplate.delete(dishKey(categoryId));
    }

    public void evictSetmeals(Long categoryId) {
        redisTemplate.delete(setmealKey(categoryId));
    }

    public void evictAllDishes() {
        deleteByPattern(CacheKeyConstant.DISH_CATEGORY_PREFIX + "*");
    }

    public void evictAllSetmeals() {
        deleteByPattern(CacheKeyConstant.SETMEAL_CATEGORY_PREFIX + "*");
    }

    private void cache(String key, List<?> values) {
        redisTemplate.opsForValue().set(key, values, expiration(values == null || values.isEmpty()));
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

    private void deleteByPattern(String pattern) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            deleteMatchingKeys(connection, pattern);
            return null;
        });
    }

    private void deleteMatchingKeys(RedisConnection connection, String pattern) {
        List<byte[]> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }

        if (!keys.isEmpty()) {
            connection.del(keys.toArray(new byte[0][]));
        }
    }
}
