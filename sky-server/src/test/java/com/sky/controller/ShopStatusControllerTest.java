package com.sky.controller;

import com.sky.constant.ShopStatusConstant;
import com.sky.result.Result;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopStatusControllerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private com.sky.controller.admin.ShopController adminController;
    private com.sky.controller.user.ShopController userController;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        adminController = new com.sky.controller.admin.ShopController();
        userController = new com.sky.controller.user.ShopController();
        ReflectionTestUtils.setField(adminController, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(userController, "redisTemplate", redisTemplate);
    }

    @Test
    void missingStatusDefaultsToOpenWithoutWritingRedis() {
        when(valueOperations.get(ShopStatusConstant.KEY)).thenReturn(null);

        Result<Integer> adminResult = adminController.getStatus();
        Result<Integer> userResult = userController.getStatus();

        assertNotNull(adminResult);
        assertNotNull(userResult);
        assertEquals(ShopStatusConstant.DEFAULT_STATUS, adminResult.getData());
        assertEquals(ShopStatusConstant.DEFAULT_STATUS, userResult.getData());
        verify(valueOperations, never()).set(eq(ShopStatusConstant.KEY), any());
    }

    @Test
    void storedOpenStatusIsReturnedUnchanged() {
        when(valueOperations.get(ShopStatusConstant.KEY)).thenReturn(1);

        assertEquals(1, adminController.getStatus().getData());
        assertEquals(1, userController.getStatus().getData());
    }

    @Test
    void storedClosedStatusIsReturnedUnchanged() {
        when(valueOperations.get(ShopStatusConstant.KEY)).thenReturn(0);

        assertEquals(0, adminController.getStatus().getData());
        assertEquals(0, userController.getStatus().getData());
    }

    @Test
    void adminStatusChangeIsVisibleToUserController() {
        AtomicReference<Object> storedStatus = new AtomicReference<>();
        when(valueOperations.get(ShopStatusConstant.KEY)).thenAnswer(invocation -> storedStatus.get());
        doAnswer(invocation -> {
            storedStatus.set(invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(eq(ShopStatusConstant.KEY), any());

        adminController.setStatus(0);

        assertEquals(0, userController.getStatus().getData());
        verify(valueOperations).set(ShopStatusConstant.KEY, 0);
    }
}
