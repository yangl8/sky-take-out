package com.sky.controller.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController 
@RequestMapping("/user/shoppingCart")
@Slf4j 
@Api(tags = "Customer cart endpoints")
public class ShoppingCartController {
    
    @Autowired 
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    @ApiOperation("Add item to cart")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("Added cart item: {}",shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @PostMapping("/sub")
    @ApiOperation("Decrease cart item quantity")
    public Result sub(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("Decreased cart item quantity: {}", shoppingCartDTO);
        shoppingCartService.subtractShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("View cart")
    public Result<List<ShoppingCart>> list(){
        List<ShoppingCart> list = shoppingCartService.showShoppingCart();
        return Result.success(list);
    }

    @DeleteMapping("/clean")
    @ApiOperation("Clear cart")
    public Result clean(){
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }
} 
