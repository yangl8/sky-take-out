package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import java.util.List;

public interface SetmealService {

    /**
     * Saves a set meal and its dish associations in one transaction, then invalidates the category cache
     *
     * @param setmealDTO Set meal data and associated dishes
     */
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * Returns a page of set meals matching the supplied filters
     *
     * @param setmealPageQueryDTO Pagination and set-meal filters
     * @return The matching page
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * Deletes set meals that are not currently on sale and invalidates affected category caches
     *
     * @param ids Set-meal IDs to delete
     */
    void deleteBatch(List<Long> ids);

    /**
     * Retrieves a set meal with its associated dishes
     *
     * @param id Set-meal ID
     * @return Set-meal details and associated dishes
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * Replaces a set meal's dish associations and invalidates the old and new category caches
     *
     * @param setmealDTO Updated set meal data and associated dishes
     */
    void update(SetmealDTO setmealDTO);

    /**
     * Changes whether a set meal is available for sale
     *
     * @param status New sale status
     * @param id Set-meal ID
     */
    void startOrStop(Integer status, Long id);

    /**
     * Lists set meals matching the supplied criteria
     *
     * @param setmeal Set-meal filters
     * @return Matching set meals
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * Lists dish options associated with a set meal
     *
     * @param id Set-meal ID
     * @return Associated dish options
     */
    List<DishItemVO> getDishItemById(Long id);
}
