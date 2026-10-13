package com.sky.controller.user;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sky.result.Result;
import com.sky.constant.ShopStatusConstant;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Api(tags = "Shop endpoints")
@Slf4j 
public class ShopController {
    
    public static final String KEY = ShopStatusConstant.KEY;

    @Autowired 
    private RedisTemplate redisTemplate;

    @Value("${sky.shop.name}")
    private String shopName;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.shop.phone}")
    private String shopPhone;

    @Value("${sky.shop.delivery-fee}")
    private BigDecimal deliveryFee;

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

    @GetMapping("/getMerchantInfo")
    @ApiOperation("Get basic shop information")
    public Result<Map<String, Object>> getMerchantInfo(){
        Map<String, Object> merchantInfo = new HashMap<>();
        merchantInfo.put("shopName", shopName);
        merchantInfo.put("shopAddress", shopAddress);
        merchantInfo.put("phone", shopPhone);
        merchantInfo.put("deliveryFee", deliveryFee);
        return Result.success(merchantInfo);
    }
}
