package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import java.util.List;

public interface CategoryService {

    /**
     * Creates a category
     *
     * @param categoryDTO Category data to save
     */
    void save(CategoryDTO categoryDTO);

    /**
     * Returns a page of categories matching the supplied filters
     *
     * @param categoryPageQueryDTO Pagination and category filters
     * @return The matching page
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * Deletes a category that is not associated with dishes or set meals
     *
     * @param id Category ID
     */
    void deleteById(Long id);

    /**
     * Updates a category
     *
     * @param categoryDTO Updated category data
     */
    void update(CategoryDTO categoryDTO);

    /**
     * Changes whether a category is enabled
     *
     * @param status New category status
     * @param id Category ID
     */
    void startOrStop(Integer status, Long id);

    /**
     * Lists categories, optionally filtered by type
     *
     * @param type Optional category type
     * @return Matching categories
     */
    List<Category> list(Integer type);
}
