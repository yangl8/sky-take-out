package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.MenuCacheService;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishServiceImplTest {

    @Mock
    private DishMapper dishMapper;
    @Mock
    private DishFlavorMapper dishFlavorMapper;
    @Mock
    private SetmealDishMapper setmealDishMapper;
    @Mock
    private SetmealMapper setmealMapper;
    @Mock
    private MenuCacheService menuCacheService;

    @InjectMocks
    private DishServiceImpl dishService;

    private DishDTO dishDTO;

    @BeforeEach
    void setUp() {
        dishDTO = new DishDTO();
        dishDTO.setId(1L);
        dishDTO.setCategoryId(20L);
        dishDTO.setFlavors(Collections.emptyList());
    }

    @Test
    void updateWithFlavorShouldEvictOldAndNewCategoryCachesWhenCategoryChanges() {
        when(dishMapper.getById(1L)).thenReturn(Dish.builder().id(1L).categoryId(10L).build());

        dishService.updateWithFlavor(dishDTO);

        verify(menuCacheService).evictDishes(10L);
        verify(menuCacheService).evictDishes(20L);
    }

    @Test
    void updateWithFlavorShouldOnlyEvictOnceWhenCategoryDoesNotChange() {
        when(dishMapper.getById(1L)).thenReturn(Dish.builder().id(1L).categoryId(20L).build());

        dishService.updateWithFlavor(dishDTO);

        verify(menuCacheService).evictDishes(20L);
        verify(menuCacheService, never()).evictDishes(10L);
    }

    @Test
    void disablingDishShouldEvictDishAndAffectedSetmealCategoryCaches() {
        when(dishMapper.getById(1L)).thenReturn(Dish.builder().id(1L).categoryId(10L).build());
        when(setmealMapper.getCategoryIdsByDishId(1L)).thenReturn(Arrays.asList(30L, 40L));
        when(setmealDishMapper.getSetmealIdsByDishIds(Collections.singletonList(1L)))
            .thenReturn(Arrays.asList(100L, 101L));

        dishService.startOrStop(StatusConstant.DISABLE, 1L);

        verify(menuCacheService).evictDishes(10L);
        verify(menuCacheService).evictSetmeals(30L);
        verify(menuCacheService).evictSetmeals(40L);
        verify(setmealMapper, times(2)).update(any());
    }

    @Test
    void enablingDishShouldNotEvictSetmealCaches() {
        when(dishMapper.getById(1L)).thenReturn(Dish.builder().id(1L).categoryId(10L).build());

        dishService.startOrStop(StatusConstant.ENABLE, 1L);

        verify(menuCacheService).evictDishes(10L);
        verify(menuCacheService, never()).evictSetmeals(any());
        verify(setmealMapper, never()).getCategoryIdsByDishId(any());
    }

}
