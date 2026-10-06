package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.MenuCacheService;
import com.sky.service.OrderService;
import com.sky.vo.DishVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired 
    private MenuCacheService menuCacheService;
    @Autowired
    private OrderService orderService;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        List<DishVO> list = menuCacheService.getDishes(categoryId);
        if(list != null){
            //如果存在，直接返回，无需查询数据库
            return Result.success(addMonthlySales(list));
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        //如果不存在，查询数据库，将查询到的数据放入redis中
        list = dishService.listWithFlavor(dish);
        menuCacheService.cacheDishes(categoryId, list);

        return Result.success(addMonthlySales(list));
    }

    private List<DishVO> addMonthlySales(List<DishVO> dishes) {
        List<Long> ids = dishes.stream().map(DishVO::getId).collect(Collectors.toList());
        Map<Long, Integer> sales = orderService.getDishMonthlySales(ids);
        dishes.forEach(dish -> dish.setMonthlySales(sales.getOrDefault(dish.getId(), 0)));
        return dishes;
    }

}
