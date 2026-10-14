package com.sky.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.service.MenuCacheService;
import com.sky.vo.DishVO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DishServiceImpl implements DishService{

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishflavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private MenuCacheService menuCacheService;

    @Transactional
    public void saveWithFlavor(DishDTO dishDTO){

        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.insert(dish);

        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });
            dishflavorMapper.insertBatch(flavors);
        }

        menuCacheService.evictDishes(dishDTO.getCategoryId());
    }

    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO){
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Transactional
    public void deleteBatch(List<Long> ids){
        List<Long> categoryIds = dishMapper.getCategoryIdsByIds(ids);

        for(Long id : ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // for(Long id : ids){
        //     dishMapper.deleteById(id);
        //     dishflavorMapper.deleteByDishId(id);
        // }

        //sql : delete from dish where id in (?,?,?)
        dishMapper.deleteByIds(ids);

        //sql : delete from dish_flavor where dish_id in (?,?,?)
        dishflavorMapper.deleteByDishIds(ids);

        evictDishCategories(categoryIds);

    }


    public DishVO getByIdWithFlavor(Long id){
        Dish dish = dishMapper.getById(id);

        List<DishFlavor> dishFlavors = dishflavorMapper.getByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }
    
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO){
        Dish existingDish = dishMapper.getById(dishDTO.getId());

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);

        dishflavorMapper.deleteByDishId(dishDTO.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(flavor -> {
                flavor.setDishId(dishDTO.getId());
            });
            dishflavorMapper.insertBatch(flavors);
        }

        menuCacheService.evictDishes(existingDish.getCategoryId());
        if (!existingDish.getCategoryId().equals(dishDTO.getCategoryId())) {
            menuCacheService.evictDishes(dishDTO.getCategoryId());
        }
    }

    /**
     * Disabling a dish also disables associated set meals so unavailable items cannot remain on sale
     *
     * @param status New sale status
     * @param id Dish ID
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish existingDish = dishMapper.getById(id);
        List<Long> affectedSetmealCategoryIds = status == StatusConstant.DISABLE
            ? setmealMapper.getCategoryIdsByDishId(id)
            : new ArrayList<>();

        Dish dish = Dish.builder()
            .id(id)
            .status(status)
            .build();
        dishMapper.update(dish);

        if (status == StatusConstant.DISABLE) {
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIds != null && setmealIds.size() > 0) {
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                        .id(setmealId)
                        .status(StatusConstant.DISABLE)
                        .build();
                    setmealMapper.update(setmeal);
                }
            }
        }

        menuCacheService.evictDishes(existingDish.getCategoryId());
        evictSetmealCategories(affectedSetmealCategoryIds);
    }

    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
            .categoryId(categoryId)
            .status(StatusConstant.ENABLE)
            .build();
        return dishMapper.list(dish);
    }

    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            List<DishFlavor> flavors = dishflavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    private void evictDishCategories(List<Long> categoryIds) {
        categoryIds.forEach(menuCacheService::evictDishes);
    }

    private void evictSetmealCategories(List<Long> categoryIds) {
        categoryIds.forEach(menuCacheService::evictSetmeals);
    }
}
