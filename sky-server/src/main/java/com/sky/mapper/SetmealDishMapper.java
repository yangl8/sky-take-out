package com.sky.mapper;

import com.sky.entity.SetmealDish;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SetmealDishMapper {

    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    void insertBatch(List<SetmealDish> setmealDishes);

    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

        @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
        List<SetmealDish> getBySetmealId(Long setmealId);
}
