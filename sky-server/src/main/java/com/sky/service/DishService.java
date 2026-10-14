package com.sky.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

public interface DishService {

    /**
     * Saves a dish and its flavors in one transaction, then invalidates the category cache
     *
     * @param dishDTO Dish data and flavor options to save
     */
    public void saveWithFlavor(DishDTO dishDTO);

    /**
     * Returns a page of dishes matching the supplied filters
     *
     * @param dishPageQueryDTO Pagination and dish filters
     * @return The matching page
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * Deletes dishes that are not on sale or referenced by set meals, then invalidates affected category caches
     *
     * @param ids Dish IDs to delete
     */
    public void deleteBatch(List<Long> ids);

    /**
     * Retrieves a dish with its configured flavors
     *
     * @param id Dish ID
     * @return Dish details and available flavors
     */
    public DishVO getByIdWithFlavor(Long id);

    /**
     * Updates a dish and its flavors, then invalidates the affected category cache
     *
     * @param dishDTO Updated dish data and flavor options
     */
    public void updateWithFlavor(DishDTO dishDTO);

    /**
     * Changes whether a dish is available for sale
     *
     * @param status New sale status
     * @param id Dish ID
     */
    void startOrStop(Integer status, Long id);

    /**
     * Lists dishes in a category
     *
     * @param categoryId Category ID
     * @return Dishes in the category
     */
    public List<Dish> list(Long categoryId);

    /**
     * Lists dishes matching the supplied criteria with their flavors
     *
     * @param dish Dish filters
     * @return Matching dishes with flavor options
     */
    List<DishVO> listWithFlavor(Dish dish);
    
}
