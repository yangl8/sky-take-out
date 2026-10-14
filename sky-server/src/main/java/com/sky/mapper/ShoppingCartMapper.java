package com.sky.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.sky.entity.ShoppingCart;

@Mapper 
public interface ShoppingCartMapper {
    
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    @Select("select * from shopping_cart where id = #{id} and user_id = #{userId}")
    ShoppingCart getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart shoppingCart);

    @Insert("insert into shopping_cart " + "(name, user_id, dish_id, setmeal_id, dish_flavor, number, amount, image, create_time) " +
        "values " + "(#{name}, #{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount}, #{image}, #{createTime})")
    void insert(ShoppingCart shoppingCart);

    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);

    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(Long id);

    void insertBatch(List<ShoppingCart> shoppingCartList);
}
