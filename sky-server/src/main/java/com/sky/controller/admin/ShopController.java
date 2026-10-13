package com.sky.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.result.Result;
import com.sky.constant.ShopStatusConstant;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "Shop endpoints")
@Slf4j 
public class ShopController {
    
    public static final String KEY = ShopStatusConstant.KEY;
    
    @Autowired 
    private RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    @ApiOperation("Set shop operating status")
    public Result setStatus(@PathVariable Integer status){
        log.info("Set shop operating status to: {}", status == 1 ? "open" : "closed");
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

    @GetMapping("/status")
    @ApiOperation("Get shop operating status")
    public Result<Integer> getStatus(){
        Integer status = (Integer)redisTemplate.opsForValue().get(KEY);
        if (status == null) {
            status = ShopStatusConstant.DEFAULT_STATUS;
        }
        log.info("Shop operating status: {}", status == 1 ? "open" : "closed");
        return Result.success(status);
    }
}
