package com.sky.service.impl;

import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.MenuCacheService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetmealServiceImplTest {

    @Mock
    private SetmealMapper setmealMapper;
    @Mock
    private SetmealDishMapper setmealDishMapper;
    @Mock
    private DishMapper dishMapper;
    @Mock
    private MenuCacheService menuCacheService;

    @InjectMocks
    private SetmealServiceImpl setmealService;

    private SetmealDTO setmealDTO;

    @BeforeEach
    void setUp() {
        setmealDTO = new SetmealDTO();
        setmealDTO.setId(1L);
        setmealDTO.setCategoryId(20L);
        setmealDTO.setSetmealDishes(Collections.emptyList());
    }

    @Test
    void updateShouldEvictOldAndNewCategoryCachesWhenCategoryChanges() {
        when(setmealMapper.getById(1L)).thenReturn(Setmeal.builder().id(1L).categoryId(10L).build());

        setmealService.update(setmealDTO);

        verify(menuCacheService).evictSetmeals(10L);
        verify(menuCacheService).evictSetmeals(20L);
    }

    @Test
    void updateShouldOnlyEvictOnceWhenCategoryDoesNotChange() {
        when(setmealMapper.getById(1L)).thenReturn(Setmeal.builder().id(1L).categoryId(20L).build());

        setmealService.update(setmealDTO);

        verify(menuCacheService).evictSetmeals(20L);
        verify(menuCacheService, never()).evictSetmeals(10L);
    }

}
