package com.sky.controller.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/admin/dish")
@Api(tags = "Dish endpoints")
@Slf4j
public class DishController {
    
    @Autowired
    private DishService dishService;

    @PostMapping
    @ApiOperation("Create dish")
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("Created dish: {}", dishDTO);
        dishService.saveWithFlavor(dishDTO);

        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("Paginated dish search")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("Paginated dish search: {}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("Delete dishes in bulk")
    public Result delete(@RequestParam List<Long> ids){
        log.info("Deleted dishes in bulk: {}", ids);
        dishService.deleteBatch(ids);

        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("Get dish by ID")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("Looking up dish by ID:", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("Update dish")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("Updated dish: {}",dishDTO);
        dishService.updateWithFlavor(dishDTO);

        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("Change dish availability")
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        dishService.startOrStop(status,id);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("List dishes by category ID")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

}
