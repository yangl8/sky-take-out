package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.service.MenuCacheService;
import com.sky.service.OrderService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "Customer-facing set meal endpoints")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private MenuCacheService menuCacheService;
    @Autowired
    private OrderService orderService;

    @GetMapping("/list")
    @ApiOperation("Get set meals by category ID")
    public Result<List<Setmeal>> list(Long categoryId) {
        List<Setmeal> list = menuCacheService.getSetmeals(categoryId);
        if (list != null) {
            return Result.success(addMonthlySales(list));
        }

        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);

        list = setmealService.list(setmeal);
        menuCacheService.cacheSetmeals(categoryId, list);
        return Result.success(addMonthlySales(list));
    }

    private List<Setmeal> addMonthlySales(List<Setmeal> setmeals) {
        List<Long> ids = setmeals.stream().map(Setmeal::getId).collect(Collectors.toList());
        Map<Long, Integer> sales = orderService.getSetmealMonthlySales(ids);
        setmeals.forEach(setmeal -> setmeal.setMonthlySales(sales.getOrDefault(setmeal.getId(), 0)));
        return setmeals;
    }

    @GetMapping("/dish/{id}")
    @ApiOperation("List dishes in a set meal by set meal ID")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        List<DishItemVO> list = setmealService.getDishItemById(id);
        return Result.success(list);
    }
}
